package forpdateam.ru.forpda.rxapi.apiclasses;

import java.util.List;

import biz.source_code.miniTemplator.MiniTemplator;
import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.api.Api;
import forpdateam.ru.forpda.api.Utils;
import forpdateam.ru.forpda.api.news.models.DetailsPage;
import forpdateam.ru.forpda.api.news.models.Material;
import forpdateam.ru.forpda.api.news.models.NewsItem;
import forpdateam.ru.forpda.api.theme.models.ThemePost;
import io.reactivex.Observable;

/**
 * Created by radiationx on 26.08.17.
 */

public class NewsRx {
    public Observable<List<NewsItem>> getNews(final String category, int pageNumber) {
        return Observable.fromCallable(() -> Api.NewsApi().getNews(category, pageNumber));
    }

    public Observable<DetailsPage> getDetails(final int id) {
        return Observable.fromCallable(() -> transform(Api.NewsApi().getDetails(id)));
    }

    public static DetailsPage transform(DetailsPage page) throws Exception {
        MiniTemplator t = App.getInstance().getTemplate(App.TEMPLATE_NEWS);
        t.setVariableOpt("style_type", App.getInstance().getCssStyleType());
        t.setVariableOpt("details_title", Utils.htmlEncode(page.getTitle()));
        t.setVariableOpt("body_type", "news");
        t.setVariableOpt("details_content", page.getHtml());
        for (Material material : page.getMaterials()) {
            t.setVariableOpt("material_id", material.getId());
            t.setVariableOpt("material_image", material.getImageUrl());
            t.setVariableOpt("material_title", material.getTitle());
            t.addBlockOpt("material");
        }
        page.setHtml(t.generateOutput());
        t.reset();

        return page;
    }
}