package forpdateam.ru.forpda.ui.fragments.editpost;

import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.common.simple.SimpleTextWatcher;
import forpdateam.ru.forpda.entity.remote.editpost.EditPoll;

/**
 * Created by radiationx on 28.07.17.
 */

public class EditPollPopup {
    private BottomSheetDialog dialog;
    private View bottomSheet;

    private TextView pollTitle;
    private EditText pollTitleField;
    private ImageButton addPoll;
    private RecyclerView questionsView;

    private PollQuestionsAdapter questionsAdapter;
    private EditPoll poll;


    public EditPollPopup(Context context) {
        dialog = new BottomSheetDialog(context);
        dialog.setOnShowListener(dialog1 -> {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });

        bottomSheet = View.inflate(context, R.layout.edit_poll, null);

        pollTitle = (TextView) bottomSheet.findViewById(R.id.poll_title);
        pollTitleField = (EditText) bottomSheet.findViewById(R.id.poll_title_field);
        addPoll = (ImageButton) bottomSheet.findViewById(R.id.add_poll);
        questionsView = (RecyclerView) bottomSheet.findViewById(R.id.poll_questions);

        questionsView.setLayoutManager(new LinearLayoutManager(questionsView.getContext()));

        addPoll.setOnClickListener(v -> {
            questionsAdapter.add(new EditPoll.Question());
        });
        pollTitleField.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (poll != null) {
                    poll.setTitle(s.toString());
                }
            }
        });


        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                }
                //window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }*/
    }

    public void show() {
        if (bottomSheet != null && bottomSheet.getParent() != null && bottomSheet.getParent() instanceof ViewGroup) {
            ((ViewGroup) bottomSheet.getParent()).removeView(bottomSheet);
        }
        dialog.setContentView(bottomSheet);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        dialog.show();
    }

    public void setPoll(EditPoll poll) {
        this.poll = poll;
        pollTitleField.setText(poll.getTitle());
        questionsAdapter = new PollQuestionsAdapter(poll.getQuestions(), poll);
        questionsView.setAdapter(questionsAdapter);
    }

}
