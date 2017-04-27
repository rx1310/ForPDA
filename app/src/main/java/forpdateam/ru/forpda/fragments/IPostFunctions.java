package forpdateam.ru.forpda.fragments;

import android.widget.Toast;

import java.util.Locale;

import forpdateam.ru.forpda.TabManager;
import forpdateam.ru.forpda.api.IBaseForumPost;
import forpdateam.ru.forpda.fragments.theme.ThemeDialogsHelper;
import forpdateam.ru.forpda.fragments.theme.editpost.EditPostFragment;
import forpdateam.ru.forpda.utils.Utils;

/**
 * Created by radiationx on 27.04.17.
 */

public interface IPostFunctions {
    String JS_POSTS_FUNCTIONS = "IPostFunctions";

    void showUserMenu(IBaseForumPost post);

    void showReputationMenu(IBaseForumPost post);

    void showPostMenu(IBaseForumPost post);

    void reportPost(IBaseForumPost post);

    void insertNick(IBaseForumPost post);

    void quotePost(String text, IBaseForumPost post);

    void deletePost(IBaseForumPost post);

    void editPost(IBaseForumPost post);

    void votePost(IBaseForumPost post, boolean type);

    void changeReputation(IBaseForumPost post, boolean type);
}
