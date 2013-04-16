package jp.redmine.redmineclient;

import java.util.List;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import jp.redmine.redmineclient.activity.helper.ActivityHelper;
import jp.redmine.redmineclient.adapter.RedmineProjectListAdapter;
import jp.redmine.redmineclient.db.cache.DatabaseCacheHelper;
import jp.redmine.redmineclient.entity.RedmineProject;
import jp.redmine.redmineclient.form.RedmineBaseAdapterListFormHelper;
import jp.redmine.redmineclient.intent.ConnectionIntent;
import jp.redmine.redmineclient.intent.ProjectIntent;
import jp.redmine.redmineclient.model.ConnectionModel;
import jp.redmine.redmineclient.task.SelectProjectTask;
import android.content.Context;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class ProjectListActivity extends OrmLiteBaseActivity<DatabaseCacheHelper>  {
	public ProjectListActivity(){
		super();
	}

	private SelectDataTask task;
	private RedmineBaseAdapterListFormHelper<RedmineProjectListAdapter> formList;

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
		setContentView(R.layout.connectionlist);

		formList = new RedmineBaseAdapterListFormHelper<RedmineProjectListAdapter>();
		formList.setList((ListView)findViewById(R.id.listConnectionList));
		formList.setFooter(getLayoutInflater().inflate(R.layout.listview_footer,null), false);
		formList.setAdapter(new RedmineProjectListAdapter(getHelper()));
		formList.onRestoreInstanceState(savedInstanceState);

		//リスト項目がクリックされた時の処理
		formList.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
				ListView listView = (ListView) parent;
				RedmineProject item = (RedmineProject) listView.getItemAtPosition(position);
				onItemSelect(item);
			}

		});

		/*
		//リスト項目が長押しされた時の処理
		formList.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				ListView listView = (ListView) parent;
				RedmineConnection item = (RedmineConnection) listView.getItemAtPosition(position);
				Bundle bundle = new Bundle();
				bundle.putInt(DIALOG_PARAM_ID, item.Id());
				bundle.putString(DIALOG_PARAM_NAME, item.Name());
				showDialog(DIALOG_ITEM_ACTION, bundle);
				return false;
			}
		});
		*/
	}

	@Override
	protected void onStart() {
		super.onStart();
		ConnectionIntent intent = new ConnectionIntent(getIntent());
		formList.adapter.setupParameter(intent.getConnectionId());
		formList.refresh();
		if(formList.adapter.getCount() == 0){
			onRefresh();
		}
	}

	protected void onItemSelect(RedmineProject item) {
		ProjectIntent intent = new ProjectIntent( getApplicationContext(), IssueListActivity.class );
		intent.setConnectionId(item.getConnectionId());
		intent.setProjectId(item.getId());
		startActivity( intent.getIntent() );
	}
	protected void onRefresh(){
		if(task != null && task.getStatus() == Status.RUNNING){
			return;
		}
		task = new SelectDataTask(this);
		task.execute();
	}

	private class SelectDataTask extends SelectProjectTask {
		public SelectDataTask(final Context tex){
			helper = getHelper();
			ConnectionIntent intent = new ConnectionIntent(getIntent());
			int id = intent.getConnectionId();
			ConnectionModel mConnection = new ConnectionModel(tex);
			connection = mConnection.getItem(id);
			mConnection.finalize();
		}
		// can use UI thread here
		@Override
		protected void onPreExecute() {
			formList.setFooterViewVisible(true);
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(List<RedmineProject> b) {
			formList.setFooterViewVisible(false);
			formList.refresh(false);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu( menu );

		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.projects, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch ( item.getItemId() )
		{
			case R.id.menu_projects_refresh:
			{
				this.onRefresh();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}
}
