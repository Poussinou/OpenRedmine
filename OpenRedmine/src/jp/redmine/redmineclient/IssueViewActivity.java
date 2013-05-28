package jp.redmine.redmineclient;

import java.math.BigDecimal;
import java.sql.SQLException;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import jp.redmine.redmineclient.activity.helper.ActionActivityHelper;
import jp.redmine.redmineclient.activity.helper.ActivityHelper;
import jp.redmine.redmineclient.adapter.RedmineJournalListAdapter;
import jp.redmine.redmineclient.db.cache.DatabaseCacheHelper;
import jp.redmine.redmineclient.db.cache.RedmineIssueModel;
import jp.redmine.redmineclient.db.cache.RedmineTimeEntryModel;
import jp.redmine.redmineclient.entity.RedmineConnection;
import jp.redmine.redmineclient.entity.RedmineIssue;
import jp.redmine.redmineclient.entity.RedmineJournal;
import jp.redmine.redmineclient.form.RedmineBaseAdapterListFormHelper;
import jp.redmine.redmineclient.form.RedmineIssueCommentForm;
import jp.redmine.redmineclient.form.RedmineIssueViewDetailForm;
import jp.redmine.redmineclient.form.RedmineIssueViewForm;
import jp.redmine.redmineclient.intent.IssueIntent;
import jp.redmine.redmineclient.model.ConnectionModel;
import jp.redmine.redmineclient.task.SelectIssueJournalPost;
import jp.redmine.redmineclient.task.SelectIssueJournalTask;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class IssueViewActivity extends OrmLiteBaseActivity<DatabaseCacheHelper>  {
	public IssueViewActivity(){
		super();
	}
	private SelectDataTask task;
	private RedmineIssueViewForm form;
	private RedmineIssueViewDetailForm formDetail;
	private RedmineIssueCommentForm formComment;
	private RedmineBaseAdapterListFormHelper<RedmineJournalListAdapter> formList;
	private MenuItem menu_refresh;

	private ProgressDialog dialog;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		formList.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		formList.onRestoreInstanceState(savedInstanceState);
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onDestroy() {
		cancelTask();
		super.onDestroy();
	}
	protected void cancelTask(){
		// cleanup task
		if(task != null && task.getStatus() == Status.RUNNING){
			task.cancel(true);
		}
	}
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityHelper.setupTheme(this);
		setContentView(R.layout.issueview);
		ActionActivityHelper actionhelper = new ActionActivityHelper(this);
		formComment = new RedmineIssueCommentForm(this);
		formComment.buttonOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!formComment.Validate())
					return;
				IssueIntent intent = new IssueIntent(getIntent());

				RedmineConnection connection = null;
				ConnectionModel mConnection = new ConnectionModel(getApplicationContext());
				connection = mConnection.getItem(intent.getConnectionId());
				mConnection.finalize();

				RedmineJournal journal = new RedmineJournal();
				journal.setIssueId((long)intent.getIssueId());
				formComment.getValue(journal);

				SelectIssueJournalPost post = new SelectIssueJournalPost(getHelper(), connection){
					private boolean isSuccess = true;
					@Override
					protected void onError(Exception lasterror) {
						isSuccess = false;
						ActivityHelper.toastRemoteError(getApplicationContext(), ActivityHelper.ERROR_APP);
						super.onError(lasterror);
					}
					@Override
					protected void onErrorRequest(int statuscode) {
						isSuccess = false;
						ActivityHelper.toastRemoteError(getApplicationContext(), statuscode);
						super.onErrorRequest(statuscode);
					}
					@Override
					protected void onPreExecute() {
						dialog.show();
						super.onPreExecute();
					}
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						if (dialog.isShowing())
							dialog.dismiss();
						if(isSuccess){
							Toast.makeText(getApplicationContext(), R.string.remote_saved, Toast.LENGTH_LONG).show();
							formComment.clear();
						}
					}
				};
				post.execute(journal);
			}
		});

		formList = new RedmineBaseAdapterListFormHelper<RedmineJournalListAdapter>();
		formList.setList((ListView)findViewById(R.id.list));
		formList.setHeader(getLayoutInflater().inflate(R.layout.issueviewdetail,null), false);
		formList.setFooter(getLayoutInflater().inflate(R.layout.listview_footer,null), false);
		formList.setAdapter(new RedmineJournalListAdapter(
				getHelper()
				, actionhelper
				));
		formList.onRestoreInstanceState(savedInstanceState);

		form = new RedmineIssueViewForm(this);
		formDetail = new RedmineIssueViewDetailForm(formList.viewHeader);
		formDetail.setupWebView(actionhelper);

		formDetail.linearTimeEntry.setOnClickListener(new android.view.View.OnClickListener() {

			@Override
			public void onClick(View v) {
				IssueIntent baseintent = new IssueIntent(getIntent());
				IssueIntent intent = new IssueIntent(getApplicationContext(), TimeEntryViewActivity.class );
				intent.setConnectionId(baseintent.getConnectionId());
				intent.setIssueId(baseintent.getIssueId());
				startActivity( intent.getIntent() );
			}
		});

		dialog = new ProgressDialog(this);
		dialog.setMessage(getString(R.string.menu_settings_uploading));
	}

	@Override
	protected void onStart() {
		super.onStart();
		onRefresh(true);
	}

	protected void onRefresh(boolean isFetch){
		IssueIntent intent = new IssueIntent(getIntent());
		int connectionid = intent.getConnectionId();

		RedmineIssueModel model = new RedmineIssueModel(getHelper());
		RedmineIssue issue = new RedmineIssue();
		Log.d("SelectDataTask","ParserIssue Start");
		try {
			issue = model.fetchById(connectionid, intent.getIssueId());
		} catch (SQLException e) {
			Log.e("SelectDataTask","ParserIssue",e);
		}

		form.setValue(issue);

		if(issue.getId() == null){
			if(isFetch){
				onFetchRemote();
			}
		} else {
			RedmineTimeEntryModel mTimeEntry = new RedmineTimeEntryModel(getHelper());
			BigDecimal hours = new BigDecimal(0);
			try {
				hours = mTimeEntry.sumByIssueId(connectionid, issue.getIssueId());
			} catch (SQLException e) {
				Log.e("SelectDataTask","ParserIssue",e);
			}
			formDetail.setValue(issue);
			formDetail.setValueTimeEntry(hours);
			formList.setHeaderViewVisible(true);

			formList.adapter.setupParameter(connectionid,issue.getId());
			formList.refresh(isFetch);

			if(formList.adapter.getCount() < 1 && isFetch){
				onFetchRemote();
			}
		}
	}

	protected void onFetchRemote(){
		if(task != null && task.getStatus() == Status.RUNNING)
			return;
		IssueIntent intent = new IssueIntent(getIntent());
		task = new SelectDataTask();
		task.execute(intent.getIssueId());
	}

	private class SelectDataTask extends SelectIssueJournalTask{
		public SelectDataTask() {
			super();
			helper = getHelper();
			IssueIntent intent = new IssueIntent(getIntent());
			int connectionid = intent.getConnectionId();
			ConnectionModel mConnection = new ConnectionModel(getApplicationContext());
			connection = mConnection.getItem(connectionid);
			mConnection.finalize();
		}
		// can use UI thread here
		@Override
		protected void onPreExecute() {
			formList.setFooterViewVisible(true);
			if(menu_refresh != null)
				menu_refresh.setEnabled(false);
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(Void v) {
			formList.setFooterViewVisible(false);
			onRefresh(false);
			if(menu_refresh != null)
				menu_refresh.setEnabled(true);
		}


	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu( menu );

		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.issue_view, menu );
		menu_refresh = menu.findItem(R.id.menu_refresh);
		if(task != null && task.getStatus() == Status.RUNNING)
			menu_refresh.setEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch ( item.getItemId() )
		{
			case R.id.menu_refresh:
			{
				onFetchRemote();
				return true;
			}
			case R.id.menu_edit:
			{
				IssueIntent baseintent = new IssueIntent(getIntent());
				IssueIntent intent = new IssueIntent(getApplicationContext(), IssueEditActivity.class );
				intent.setConnectionId(baseintent.getConnectionId());
				intent.setProjectId(baseintent.getProjectId());
				intent.setIssueId(baseintent.getIssueId());
				startActivity( intent.getIntent() );
				return true;
			}
			case R.id.menu_comment:
			{
				formComment.toggle();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}
}
