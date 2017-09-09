package forpdateam.ru.forpda.fragments.qms;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.TabManager;
import forpdateam.ru.forpda.api.qms.interfaces.IQmsTheme;
import forpdateam.ru.forpda.api.qms.models.QmsContact;
import forpdateam.ru.forpda.api.qms.models.QmsThemes;
import forpdateam.ru.forpda.data.realm.qms.QmsThemesBd;
import forpdateam.ru.forpda.fragments.ListFragment;
import forpdateam.ru.forpda.fragments.TabFragment;
import forpdateam.ru.forpda.fragments.notes.NotesAddPopup;
import forpdateam.ru.forpda.fragments.qms.adapters.QmsThemesAdapter;
import forpdateam.ru.forpda.fragments.qms.chat.QmsChatFragment;
import forpdateam.ru.forpda.rxapi.RxApi;
import forpdateam.ru.forpda.utils.AlertDialogMenu;
import forpdateam.ru.forpda.utils.IntentHandler;
import forpdateam.ru.forpda.utils.rx.Subscriber;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by radiationx on 25.08.16.
 */
public class QmsThemesFragment extends ListFragment {
    public final static String USER_ID_ARG = "USER_ID_ARG";
    public final static String USER_AVATAR_ARG = "USER_AVATAR_ARG";
    private MenuItem blackListMenuItem;
    private MenuItem noteMenuItem;
    private String avatarUrl;
    private QmsThemes currentThemes = new QmsThemes();
    private QmsThemesAdapter adapter;
    private Realm realm;
    private RealmResults<QmsThemesBd> results;
    private Subscriber<QmsThemes> mainSubscriber = new Subscriber<>(this);
    private Subscriber<ArrayList<QmsContact>> contactsSubscriber = new Subscriber<>(this);
    private AlertDialogMenu<QmsThemesFragment, IQmsTheme> dialogMenu;
    private QmsThemesAdapter.OnItemClickListener onItemClickListener =
            theme -> {
                Bundle args = new Bundle();
                args.putString(TabFragment.ARG_TITLE, theme.getName());
                args.putString(TabFragment.TAB_SUBTITLE, getTitle());
                args.putInt(QmsChatFragment.USER_ID_ARG, currentThemes.getUserId());
                args.putString(QmsChatFragment.USER_AVATAR_ARG, avatarUrl);
                args.putInt(QmsChatFragment.THEME_ID_ARG, theme.getId());
                args.putString(QmsChatFragment.THEME_TITLE_ARG, theme.getName());
                TabManager.getInstance().add(QmsChatFragment.class, args);
            };
    private QmsThemesAdapter.OnItemClickListener onLongItemClickListener =
            theme -> {
                if (dialogMenu == null) {
                    dialogMenu = new AlertDialogMenu<>();
                    dialogMenu.addItem(getString(R.string.delete), (context, data) -> {
                        mainSubscriber.subscribe(RxApi.Qms().deleteTheme(currentThemes.getUserId(), data.getId()), this::onLoadThemes, currentThemes, v -> loadData());
                    });
                    dialogMenu.addItem(getString(R.string.create_note), (context1, data) -> {
                        String title = String.format(getString(R.string.dialog_Title_Nick), data.getName(), currentThemes.getNick());
                        String url = "http://4pda.ru/forum/index.php?act=qms&mid=" + currentThemes.getUserId() + "&t=" + data.getId();
                        NotesAddPopup.showAddNoteDialog(context1.getContext(), title, url);
                    });
                }
                new AlertDialog.Builder(getContext())
                        .setItems(dialogMenu.getTitles(), (dialog, which) -> dialogMenu.onClick(which, QmsThemesFragment.this, theme)).show();
            };


    public QmsThemesFragment() {
        //configuration.setUseCache(true);
        configuration.setDefaultTitle(App.getInstance().getString(R.string.fragment_title_dialogs));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        if (getArguments() != null) {
            currentThemes.setUserId(getArguments().getInt(USER_ID_ARG));
            avatarUrl = getArguments().getString(USER_AVATAR_ARG);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        initFabBehavior();
        tryShowAvatar();
        viewsReady();


        refreshLayoutStyle(refreshLayout);
        refreshLayout.setOnRefreshListener(this::loadData);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fab.setImageDrawable(App.getVecDrawable(getContext(), R.drawable.ic_fab_create));
        fab.setOnClickListener(view1 -> {
            Bundle args = new Bundle();
            args.putInt(QmsChatFragment.USER_ID_ARG, currentThemes.getUserId());
            args.putString(QmsChatFragment.USER_NICK_ARG, currentThemes.getNick());
            args.putString(QmsChatFragment.USER_AVATAR_ARG, avatarUrl);
            TabManager.getInstance().add(QmsChatFragment.class, args);
        });
        fab.setVisibility(View.VISIBLE);
        adapter = new QmsThemesAdapter();
        adapter.setOnItemClickListener(onItemClickListener);
        adapter.setOnLongItemClickListener(onLongItemClickListener);
        recyclerView.setAdapter(adapter);
        bindView();
        return view;
    }

    private void tryShowAvatar() {
        if (avatarUrl != null) {
            ImageLoader.getInstance().displayImage(avatarUrl, toolbarImageView);
            toolbarImageView.setVisibility(View.VISIBLE);
            toolbarImageView.setOnClickListener(view1 -> IntentHandler.handle("https://4pda.ru/forum/index.php?showuser=" + currentThemes.getUserId()));
            toolbarImageView.setContentDescription(App.getInstance().getString(R.string.user_avatar));
        } else {
            toolbarImageView.setVisibility(View.GONE);
        }
    }


    @Override
    public void loadData() {
        super.loadData();
        refreshLayout.setRefreshing(true);

        refreshToolbarMenuItems(false);

        mainSubscriber.subscribe(RxApi.Qms().getThemesList(currentThemes.getUserId()), this::onLoadThemes, currentThemes, v -> loadData());
    }

    private void onLoadThemes(QmsThemes themes) {
        refreshLayout.setRefreshing(false);

        recyclerView.scrollToPosition(0);
        currentThemes = themes;

        setTabTitle(String.format(getString(R.string.dialogs_Nick), currentThemes.getNick()));
        setTitle(currentThemes.getNick());
        if (currentThemes.getThemes().size() == 0 && currentThemes.getNick() != null) {
            Bundle args = new Bundle();
            args.putInt(QmsChatFragment.USER_ID_ARG, currentThemes.getUserId());
            args.putString(QmsChatFragment.USER_NICK_ARG, currentThemes.getNick());
            args.putString(QmsChatFragment.USER_AVATAR_ARG, avatarUrl);
            TabManager.getInstance().add(QmsChatFragment.class, args);
            //new Handler().postDelayed(() -> TabManager.getInstance().remove(getTag()), 500);
            return;
        }
        if (currentThemes.getThemes().size() == 0)
            return;
        realm.executeTransactionAsync(r -> {
            r.delete(QmsThemesBd.class);
            QmsThemesBd qmsThemesBd = new QmsThemesBd(currentThemes);
            r.copyToRealmOrUpdate(qmsThemesBd);
            qmsThemesBd.getThemes().clear();
        }, this::bindView);
    }

    private void bindView() {
        if (realm.isClosed()) return;
        results = realm.where(QmsThemesBd.class).equalTo("userId", currentThemes.getUserId()).findAll();

        if (results.size() != 0 && results.last().getThemes().size() != 0) {
            adapter.addAll(results.last().getThemes());
        }
        refreshToolbarMenuItems(true);
    }

    @Override
    protected void addBaseToolbarMenu() {
        super.addBaseToolbarMenu();
        blackListMenuItem = getMenu().add(R.string.add_to_blacklist)
                .setOnMenuItemClickListener(item -> {
                    contactsSubscriber.subscribe(RxApi.Qms().blockUser(currentThemes.getNick()), qmsContacts -> {
                        if (qmsContacts.size() > 0) {
                            Toast.makeText(getContext(), R.string.user_added_to_blacklist, Toast.LENGTH_SHORT).show();
                        }
                    }, new ArrayList<>());
                    return false;
                });
        noteMenuItem = getMenu().add(R.string.create_note)
                .setOnMenuItemClickListener(item -> {
                    String title = String.format(getString(R.string.dialogs_Nick), currentThemes.getNick());
                    String url = "http://4pda.ru/forum/index.php?act=qms&mid=" + currentThemes.getUserId();
                    NotesAddPopup.showAddNoteDialog(getContext(), title, url);
                    return true;
                });
        refreshToolbarMenuItems(false);
    }

    @Override
    protected void refreshToolbarMenuItems(boolean enable) {
        super.refreshToolbarMenuItems(enable);
        if (enable) {
            blackListMenuItem.setEnabled(true);
            noteMenuItem.setEnabled(true);
        } else {
            blackListMenuItem.setEnabled(false);
            noteMenuItem.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
