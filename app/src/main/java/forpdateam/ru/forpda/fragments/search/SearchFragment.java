package forpdateam.ru.forpda.fragments.search;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.api.search.models.SearchResult;
import forpdateam.ru.forpda.api.search.models.SearchSettings;
import forpdateam.ru.forpda.fragments.TabFragment;
import forpdateam.ru.forpda.pagination.PaginationHelper;
import forpdateam.ru.forpda.rxapi.RxApi;
import forpdateam.ru.forpda.utils.ExtendedWebView;
import forpdateam.ru.forpda.utils.IntentHandler;
import forpdateam.ru.forpda.utils.Utils;
import forpdateam.ru.forpda.utils.rx.Subscriber;

/**
 * Created by radiationx on 29.01.17.
 */

public class SearchFragment extends TabFragment {
    protected final static String JS_INTERFACE = "ISearch";
    private ViewGroup searchSettingsView;
    private ViewGroup nickBlock, resourceBlock, resultBlock, sortBlock, sourceBlock;
    private Spinner resourceSpinner, resultSpinner, sortSpinner, sourceSpinner;
    private TextView nickField;
    private Button submitButton;

    private List<String> resourceItems = Arrays.asList(SearchSettings.RESOURCE_FORUM.second, SearchSettings.RESOURCE_NEWS.second);
    private List<String> resultItems = Arrays.asList(SearchSettings.RESULT_TOPICS.second, SearchSettings.RESULT_POSTS.second);
    private List<String> sortItems = Arrays.asList(SearchSettings.SORT_DA.second, SearchSettings.SORT_DD.second, SearchSettings.SORT_REL.second);
    private List<String> sourceItems = Arrays.asList(SearchSettings.SOURCE_ALL.second, SearchSettings.SOURCE_TITLES.second, SearchSettings.SOURCE_CONTENT.second);

    private SearchSettings settings = new SearchSettings();

    private Subscriber<SearchResult> mainSubscriber = new Subscriber<>(this);
    private ExtendedWebView webView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout refreshLayout;
    private SearchAdapter adapter = new SearchAdapter();

    private StringBuilder titleBuilder = new StringBuilder();
    private PaginationHelper paginationHelper = new PaginationHelper();

    public SearchFragment() {
        configuration.setDefaultTitle("Поиск");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
           /* String url = getArguments().getString(ARG_TAB);
            if (url != null)
                settings = SearchSettings.parseSettings(settings, url);*/
            settings = SearchSettings.fromBundle(settings, getArguments());
        }
    }

    private SearchView searchView;
    private MenuItem searchItem;
    private SearchResult data;

    @SuppressLint("JavascriptInterface")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        baseInflateFragment(inflater, R.layout.fragment_search);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_list);
        //recyclerView = (RecyclerView) findViewById(R.id.base_list);
        searchSettingsView = (ViewGroup) findViewById(R.id.search_settings_container);

        nickBlock = (ViewGroup) findViewById(R.id.search_nick_block);
        resourceBlock = (ViewGroup) findViewById(R.id.search_resource_block);
        resultBlock = (ViewGroup) findViewById(R.id.search_result_block);
        sortBlock = (ViewGroup) findViewById(R.id.search_sort_block);
        sourceBlock = (ViewGroup) findViewById(R.id.search_source_block);

        resourceSpinner = (Spinner) findViewById(R.id.search_resource_spinner);
        resultSpinner = (Spinner) findViewById(R.id.search_result_spinner);
        sortSpinner = (Spinner) findViewById(R.id.search_sort_spinner);
        sourceSpinner = (Spinner) findViewById(R.id.search_source_spinner);

        nickField = (TextView) findViewById(R.id.search_nick_field);

        submitButton = (Button) findViewById(R.id.search_submit);

        if (getMainActivity().getWebViews().size() > 0) {
            webView = getMainActivity().getWebViews().element();
            getMainActivity().getWebViews().remove();
        } else {
            webView = new ExtendedWebView(getContext());
            webView.setTag("WebView_tag ".concat(Long.toString(System.currentTimeMillis())));
        }
        webView.loadUrl("about:blank");
        webView.addJavascriptInterface(this, JS_INTERFACE);
        webView.getSettings().setJavaScriptEnabled(true);
        recyclerView = new RecyclerView(getContext());

        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        viewsReady();

        paginationHelper.inflatePagination(getContext(), inflater, toolbar);
        paginationHelper.setupToolbar(toolbarLayout);
        paginationHelper.setListener(new PaginationHelper.PaginationListener() {
            @Override
            public boolean onTabSelected(TabLayout.Tab tab) {
                return refreshLayout.isRefreshing();
            }

            @Override
            public void onSelectedPage(int pageNumber) {
                settings.setSt(pageNumber);
                loadData();
            }
        });

        searchSettingsView.setVisibility(View.GONE);
        toolbar.getMenu().clear();
        addBaseToolbarMenu();
        toolbar.getMenu().add("Скопировать ссылку").setOnMenuItemClickListener(menuItem -> {
            Utils.copyToClipBoard(settings.toUrl());
            return false;
        });
        toolbar.inflateMenu(R.menu.qms_contacts_menu);
        toolbar.getMenu().add("Настройки").setIcon(R.drawable.ic_tune_gray_24dp).setOnMenuItemClickListener(menuItem -> {
            if (searchSettingsView.getVisibility() == View.VISIBLE) {
                searchSettingsView.setVisibility(View.GONE);
            } else {
                searchSettingsView.setVisibility(View.VISIBLE);
                hidePopupWindows();
            }
            return false;
        }).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchItem = toolbar.getMenu().findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconifiedByDefault(true);

        setItems(resourceSpinner, (String[]) resourceItems.toArray(), 0);
        setItems(resultSpinner, (String[]) resultItems.toArray(), 0);
        setItems(sortSpinner, (String[]) sortItems.toArray(), 0);
        setItems(sourceSpinner, (String[]) sourceItems.toArray(), 1);
        fillSettingsData();

        SearchManager searchManager = (SearchManager) getMainActivity().getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getMainActivity().getComponentName()));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                startSearch();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setQueryHint("Ключевые слова");
        searchItem.expandActionView();
        submitButton.setOnClickListener(v -> startSearch());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        refreshLayout.setOnRefreshListener(this::loadData);
        adapter.setOnItemClickListener(item -> {
            if (settings.getResourceType().equals(SearchSettings.RESOURCE_NEWS.first)) {
                IntentHandler.handle("http://4pda.ru/index.php?p=" + item.getId());
            } else {
                String url = "http://4pda.ru/forum/index.php?showtopic=" + item.getTopicId();
                if (item.getId() != 0) {
                    url += "&view=findpost&p=" + item.getId();
                }
                IntentHandler.handle(url);
            }
        });
        return view;
    }

    private boolean checkArg(String arg, Pair<String, String> pair) {
        return arg.equals(pair.first);
    }

    private boolean checkName(String arg, Pair<String, String> pair) {
        return arg.equals(pair.second);
    }

    private void setSelection(Spinner spinner, List<String> items, Pair<String, String> pair) {
        spinner.setSelection(items.indexOf(pair.second));
    }

    private void fillSettingsData() {
        searchView.setQuery(settings.getQuery(), false);
        nickField.setText(settings.getNick());

        if (checkArg(settings.getResourceType(), SearchSettings.RESOURCE_NEWS)) {
            setSelection(resourceSpinner, resourceItems, SearchSettings.RESOURCE_NEWS);
        } else if (checkArg(settings.getResourceType(), SearchSettings.RESOURCE_FORUM)) {
            setSelection(resourceSpinner, resourceItems, SearchSettings.RESOURCE_FORUM);
        }

        if (checkArg(settings.getResult(), SearchSettings.RESULT_TOPICS)) {
            setSelection(resultSpinner, resultItems, SearchSettings.RESULT_TOPICS);
        } else if (checkArg(settings.getResult(), SearchSettings.RESULT_POSTS)) {
            setSelection(resultSpinner, resultItems, SearchSettings.RESULT_POSTS);
        }

        if (checkArg(settings.getSort(), SearchSettings.SORT_DA)) {
            setSelection(sortSpinner, sortItems, SearchSettings.SORT_DA);
        } else if (checkArg(settings.getSort(), SearchSettings.SORT_DD)) {
            setSelection(sortSpinner, sortItems, SearchSettings.SORT_DD);
        } else if (checkArg(settings.getSort(), SearchSettings.SORT_REL)) {
            setSelection(sortSpinner, sortItems, SearchSettings.SORT_REL);
        }

        if (checkArg(settings.getSource(), SearchSettings.SOURCE_ALL)) {
            setSelection(sourceSpinner, sourceItems, SearchSettings.SOURCE_ALL);
        } else if (checkArg(settings.getSource(), SearchSettings.SOURCE_TITLES)) {
            setSelection(sourceSpinner, sourceItems, SearchSettings.SOURCE_TITLES);
        } else if (checkArg(settings.getSource(), SearchSettings.SOURCE_CONTENT)) {
            setSelection(sourceSpinner, sourceItems, SearchSettings.SOURCE_CONTENT);
        }
    }

    private AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String arg;
            if (parent == resourceSpinner) {
                arg = resourceItems.get(position);
                if (checkName(arg, SearchSettings.RESOURCE_NEWS)) {
                    settings.setResourceType(SearchSettings.RESOURCE_NEWS.first);
                    setNewsMode();
                } else if (checkName(arg, SearchSettings.RESOURCE_FORUM)) {
                    settings.setResourceType(SearchSettings.RESOURCE_FORUM.first);
                    setForumMode();
                }
            } else if (parent == resultSpinner) {
                arg = resultItems.get(position);
                if (checkName(arg, SearchSettings.RESULT_TOPICS)) {
                    settings.setResult(SearchSettings.RESULT_TOPICS.first);
                } else if (checkName(arg, SearchSettings.RESULT_POSTS)) {
                    settings.setResult(SearchSettings.RESULT_POSTS.first);
                }
            } else if (parent == sortSpinner) {
                arg = sortItems.get(position);
                if (checkName(arg, SearchSettings.SORT_DA)) {
                    settings.setSort(SearchSettings.SORT_DA.first);
                } else if (checkName(arg, SearchSettings.SORT_DD)) {
                    settings.setSort(SearchSettings.SORT_DD.first);
                } else if (checkName(arg, SearchSettings.SORT_REL)) {
                    settings.setSort(SearchSettings.SORT_REL.first);
                }
            } else if (parent == sourceSpinner) {
                arg = sourceItems.get(position);
                if (checkName(arg, SearchSettings.SOURCE_ALL)) {
                    settings.setSource(SearchSettings.SOURCE_ALL.first);
                } else if (checkName(arg, SearchSettings.SOURCE_TITLES)) {
                    settings.setSource(SearchSettings.SOURCE_TITLES.first);
                } else if (checkName(arg, SearchSettings.SOURCE_CONTENT)) {
                    settings.setSource(SearchSettings.SOURCE_CONTENT.first);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private void setNewsMode() {
        nickBlock.setVisibility(View.GONE);
        resultBlock.setVisibility(View.GONE);
        sortBlock.setVisibility(View.GONE);
        sourceBlock.setVisibility(View.GONE);
    }

    private void setForumMode() {
        nickBlock.setVisibility(View.VISIBLE);
        resultBlock.setVisibility(View.VISIBLE);
        sortBlock.setVisibility(View.VISIBLE);
        sourceBlock.setVisibility(View.VISIBLE);
    }

    private void setItems(Spinner spinner, String[] items, int selection) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getMainActivity(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selection);
        spinner.setOnItemSelectedListener(listener);
    }

    private void startSearch() {
        settings.setSt(0);
        settings.setQuery(searchView.getQuery().toString());
        settings.setNick(nickField.getText().toString());
        loadData();
    }

    private void buildTitle() {
        titleBuilder.setLength(0);
        titleBuilder.append("Поиск");
        if (settings.getResourceType().equals(SearchSettings.RESOURCE_NEWS.first)) {
            titleBuilder.append(" новостей");
        } else {
            if (settings.getResult().equals(SearchSettings.RESULT_POSTS.first)) {
                titleBuilder.append(" сообщений");
            } else {
                titleBuilder.append(" тем");
            }
            if (!settings.getNick().isEmpty()) {
                titleBuilder.append(" пользователя \"").append(settings.getNick()).append("\"");
            }
        }
        if (!settings.getQuery().isEmpty()) {
            titleBuilder.append(" по запросу \"").append(settings.getQuery()).append("\"");
        }
        setTitle(titleBuilder.toString());
    }

    @Override
    public void loadData() {
        if (settings.getQuery().isEmpty() && settings.getNick().isEmpty()) {
            return;
        }
        buildTitle();
        hidePopupWindows();
        if (searchSettingsView.getVisibility() == View.VISIBLE) {
            searchSettingsView.setVisibility(View.GONE);
        }
        refreshLayout.setRefreshing(true);
        boolean withHtml = settings.getResult().equals(SearchSettings.RESULT_POSTS.first) && settings.getResourceType().equals(SearchSettings.RESOURCE_FORUM.first);
        mainSubscriber.subscribe(RxApi.Search().getSearch(settings, withHtml), this::onLoadData, new SearchResult(), v -> loadData());
    }

    private void onLoadData(SearchResult searchResult) {
        refreshLayout.setRefreshing(false);
        hidePopupWindows();
        data = searchResult;
        Log.e("SUKA", data.getSettings().getResult());
        Log.e("SUKA", data.getSettings().getResourceType());
        Log.e("SUKA", "" + refreshLayout.getChildCount());
        if (refreshLayout.getChildCount() > 1) {
            Log.e("SUKA", "" + refreshLayout.getChildAt(0));
        }
        if (data.getSettings().getResult().equals(SearchSettings.RESULT_POSTS.first) && data.getSettings().getResourceType().equals(SearchSettings.RESOURCE_FORUM.first)) {
            if (refreshLayout.getChildCount() > 1) {
                if (refreshLayout.getChildAt(0) instanceof RecyclerView) {
                    refreshLayout.removeViewAt(0);
                    fixTargetView();
                    refreshLayout.addView(webView);
                    Log.e("SUKA", "add webview");
                }
            } else {
                refreshLayout.addView(webView);
                Log.e("SUKA", "add webview");
            }
            webView.loadDataWithBaseURL("http://4pda.ru/forum/", data.getHtml(), "text/html", "utf-8", null);
        } else {
            if (refreshLayout.getChildCount() > 1) {
                if (refreshLayout.getChildAt(0) instanceof ExtendedWebView) {
                    refreshLayout.removeViewAt(0);
                    fixTargetView();
                    refreshLayout.addView(recyclerView);
                    Log.e("SUKA", "add recyclerview");
                }
            } else {
                refreshLayout.addView(recyclerView);
                Log.e("SUKA", "add recyclerview");
            }
            adapter.clear();
            adapter.addAll(data.getItems());
        }

        paginationHelper.updatePagination(data.getPagination());
        setSubtitle(paginationHelper.getString());
    }


    //Поле mTarget это вьюха, от которой зависит обработка движений
    private void fixTargetView() {
        try {
            Field field = refreshLayout.getClass().getDeclaredField("mTarget");
            field.setAccessible(true);
            field.set(refreshLayout, null);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
