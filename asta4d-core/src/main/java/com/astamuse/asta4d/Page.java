package com.astamuse.asta4d;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;

import com.astamuse.asta4d.interceptor.PageInterceptor;
import com.astamuse.asta4d.interceptor.base.Executor;
import com.astamuse.asta4d.interceptor.base.GenericInterceptor;
import com.astamuse.asta4d.interceptor.base.InterceptorUtil;
import com.astamuse.asta4d.render.GoThroughRenderer;
import com.astamuse.asta4d.render.Renderer;
import com.astamuse.asta4d.template.Template;
import com.astamuse.asta4d.template.TemplateException;
import com.astamuse.asta4d.template.TemplateResolver;
import com.astamuse.asta4d.util.RenderUtil;

public class Page {

    private static class PageInterceptorWrapper implements GenericInterceptor<Document> {

        private PageInterceptor interceptor;

        public PageInterceptorWrapper(PageInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        @Override
        public boolean beforeProcess(Document doc) throws Exception {
            Renderer renderer = new GoThroughRenderer();
            boolean result = interceptor.prePageRendering(renderer);
            if (result) {
                RenderUtil.apply(doc, renderer);
            }
            return result;
        }

        @Override
        public void afterProcess(Document doc, Exception ex) throws Exception {
            if (ex != null) {
                return;
            }
            Renderer renderer = new GoThroughRenderer();
            interceptor.postPageRendering(renderer);
            RenderUtil.apply(doc, renderer);
        }

        public final static List<PageInterceptorWrapper> buildList(List<PageInterceptor> interceptorList) {
            List<PageInterceptorWrapper> list = new ArrayList<>();
            if (interceptorList != null) {
                for (PageInterceptor interceptor : interceptorList) {
                    list.add(new PageInterceptorWrapper(interceptor));
                }
            }
            return list;
        }

    }

    protected Template template;

    public Page(String path) throws TemplateException {
        Configuration conf = Context.getCurrentThreadContext().getConfiguration();
        TemplateResolver templateResolver = conf.getTemplateResolver();
        template = templateResolver.findTemplate(path);
    }

    public void output(OutputStream out) throws Exception {
        List<PageInterceptor> interceptorList = Context.getCurrentThreadContext().getConfiguration().getPageInterceptorList();
        List<PageInterceptorWrapper> wrapperList = PageInterceptorWrapper.buildList(interceptorList);

        Document doc = template.getDocumentClone();

        InterceptorUtil.executeWithInterceptors(doc, wrapperList, new Executor<Document>() {
            @Override
            public void execute(Document doc) throws Exception {
                RenderUtil.applySnippets(doc);
                RenderUtil.applyClearAction(doc, true);
            }
        });

        RenderUtil.applyClearAction(doc, true);
        out.write(doc.outerHtml().getBytes("utf-8"));
    }

    public String output() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            output(bos);
            return bos.toString("utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();

        }

    }
}
