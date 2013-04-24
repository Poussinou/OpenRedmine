package jp.redmine.redmineclient.external.lib;

import jp.redmine.redmineclient.R;
import jp.redmine.redmineclient.form.HttpAuthDialogForm;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyWebViewClient extends WebViewClient {

	private String loginCookie;
	private Context mContext;
	private WebView mWebView;
	public String UserID = "";
	public String Password = "";
	public boolean isSSLError = false;
	protected CookieManager cookieManager;

	public MyWebViewClient(Context context, WebView webview) {
		super();

		mContext = context;
		mWebView = webview;
	}

	public void Destroy(){
		if(cookieManager!=null){
			cookieManager.removeSessionCookie();
			cookieManager = null;
		}
	}

	@Override
	public void onPageFinished( WebView view, String url ) {
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setCookie(url, loginCookie);
	}

	@Override
	public void onLoadResource( WebView view, String url ){
		cookieManager = CookieManager.getInstance();
		loginCookie = cookieManager.getCookie(url);
	}

	@Override
	public boolean shouldOverrideUrlLoading( WebView view, String url ) {
		return false;
	}

	@Override
	public void onReceivedSslError( WebView view, SslErrorHandler handler, SslError error ) {
		isSSLError = true;
		handler.proceed();
	}

	@Override
	public void onReceivedHttpAuthRequest( WebView view, final HttpAuthHandler handler, final String host, final String realm ){
		// 引数hostにはsetHttpAuthUsernamePasswordの第1引数で設定した文字列が入ってきます。
		// Android 4.*（もしかすると3.*から）ではなぜか引数hostに勝手にポート番号が追記されてしまいます。
		// 具体的には「:80」が追記されてしまいます。

		String userName = null;
		String userPass = null;

		if (handler.useHttpAuthUsernamePassword() && view != null) {
			String[] haup = view.getHttpAuthUsernamePassword(host, realm);
			if (haup != null && haup.length == 2) {
				userName = haup[0];
				userPass = haup[1];
			}
		}

		if (userName != null && userPass != null) {
			afterSetHttpAuth(userName,userPass);
			handler.proceed(userName, userPass);
		}
		else {
			showHttpAuthDialog(handler, host, realm, null);
		}
	}

	private void showHttpAuthDialog( final HttpAuthHandler handler, final String host, final String realm, final String title ) {
		LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
		View view = inflater.inflate(R.layout.basicauthentication, null);

		final HttpAuthDialogForm form = new HttpAuthDialogForm(view);
		form.setUserID(UserID);
		form.setPassword(Password);
		final Builder mHttpAuthDialog = new AlertDialog.Builder((Activity)mContext);

		mHttpAuthDialog.setTitle(view.getContext().getString(R.string.menu_setting_check_auth))
			.setView(view)
			.setCancelable(false)
			.setPositiveButton(view.getContext().getString(R.string.button_ok)
				, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					String userName = form.getUserID();
					String userPass = form.getPassword();

					mWebView.setHttpAuthUsernamePassword(host, realm, userName, userPass);

					afterSetHttpAuth(userName,userPass);
					handler.proceed(userName, userPass);
				}
			})
			.setNegativeButton(view.getContext().getString(R.string.button_cancel)
				, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					handler.cancel();
				}
			})
			.create().show();
	}

	protected void afterSetHttpAuth(String id, String password){

	}

}

