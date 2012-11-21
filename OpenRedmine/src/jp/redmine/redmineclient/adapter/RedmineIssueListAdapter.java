package jp.redmine.redmineclient.adapter;

import java.util.ArrayList;

import jp.redmine.redmineclient.R;
import jp.redmine.redmineclient.entity.RedmineIssue;
import jp.redmine.redmineclient.form.RedmineIssueListItemForm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class RedmineIssueListAdapter extends ArrayAdapter<RedmineIssue> {
	private ArrayList<RedmineIssue> items;
	private LayoutInflater inflater;
	/**
	 * Setup the class
	 * @param context Application context
	 * @param textViewResourceId Resource ID
	 * @param items list of issues
	 */
	public RedmineIssueListAdapter(Context context, int textViewResourceId,
	ArrayList<RedmineIssue> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// ビューを受け取る
		View view = convertView;
		if (convertView == null) {
			view = inflater.inflate(R.layout.issueitem, null);
		}
		// 表示すべきデータの取得
		RedmineIssue item = (RedmineIssue)items.get(position);
		renderIssue(view,item);
		return view;
	}
	/**
	 * Render issue to the view
	 * @param view
	 * @param item issue item
	 */
	protected void renderIssue(View view, RedmineIssue item){
		if (item == null)
			return;
		// uses cache
		RedmineIssueListItemForm form = (RedmineIssueListItemForm)view.getTag();
		if(form == null){
			form = new RedmineIssueListItemForm(view);
			view.setTag(form);
		}
		form.setValue(item);

	}
}