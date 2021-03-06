package com.ghyeok.plugin.kakao;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.message.template.*;
import com.kakao.network.callback.*;
import com.kakao.auth.ApprovalType;
import com.kakao.auth.AuthType;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.ISessionConfig;
import com.kakao.auth.KakaoAdapter;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.KakaoParameterException;
import com.kakao.util.exception.KakaoException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class KakaoTalk extends CordovaPlugin {

    private static final String LOG_TAG = "KakaoTalk";
    private static volatile Activity currentActivity;
    private SessionCallback callback;

    /**
     * Initialize cordova plugin kakaotalk
     *
     * @param cordova
     * @param webView
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.v(LOG_TAG, "kakao : initialize");
        super.initialize(cordova, webView);
        currentActivity = this.cordova.getActivity();
        KakaoSDK.init(new KakaoSDKAdapter());
    }

    /**
     * Execute plugin
     *
     * @param action
     * @param options
     * @param callbackContext
     */
    public boolean execute(final String action, JSONArray options, final CallbackContext callbackContext) throws JSONException {
        Log.v(LOG_TAG, "kakao : execute " + action);
        cordova.setActivityResultCallback(this);
        callback = new SessionCallback(callbackContext);
        Session.getCurrentSession().addCallback(callback);

        if (action.equals("login")) {
            this.login();
            // this.getAccessToken(callbackContext);
            //requestMe(callbackContext);
            return true;
        } else if (action.equals("logout")) {
            this.logout(callbackContext);
            return true;
	    } else if (action.equals("getAccessToken")) {
	        this.getAccessToken(callbackContext);
	        return true;
        } else if (action.equals("share")) {

            try {
                this.share(options, callbackContext);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void share(JSONArray options, final CallbackContext callbackContext) throws KakaoParameterException {

        try {

            final JSONObject parameters = options.getJSONObject(0);
            String webLinkText = "";
            String webLinkUrl = "";
            String appLinkText = "";
            String appLinkUrl = "";
            String paramsTitle = "";
            String paramsDesc= "";
            String paramsImageUrl = "";
            String paramsLink = "";

            if (parameters.has("text")) {
                // TODO: 
            }

            if (parameters.has("params")) {
                JSONObject paramsObj = parameters.getJSONObject("params");
                if(paramsObj.has("title") && paramsObj.has("desc") && paramsObj.has("imageUrl") && paramsObj.has("link")) {
                    paramsTitle = paramsObj.getString("title");
                    paramsDesc = paramsObj.getString("desc");
                    paramsImageUrl = paramsObj.getString("imageUrl");
                    paramsLink = paramsObj.getString("link");
                }
            }

            if (parameters.has("weblink")) {
                JSONObject weblinkObj = parameters.getJSONObject("weblink");
                if(weblinkObj.has("text") && weblinkObj.has("url")) {
                    webLinkText = weblinkObj.getString("text");
                    webLinkUrl = weblinkObj.getString("url");
                }
            }

            if (parameters.has("applink")) {
                JSONObject applinkObj = parameters.getJSONObject("applink");
                if(applinkObj.has("text") && applinkObj.has("url")) {
                    appLinkText = applinkObj.getString("text");
                    appLinkUrl = applinkObj.getString("url");
                }
            }

            FeedTemplate params = FeedTemplate.newBuilder(ContentObject
                    .newBuilder(paramsTitle, paramsImageUrl, LinkObject.newBuilder().setWebUrl(paramsLink).setMobileWebUrl(paramsLink).build())
                    .setDescrption(paramsDesc).build())
                    .addButton(new ButtonObject(webLinkText, LinkObject.newBuilder().setWebUrl(webLinkUrl).setMobileWebUrl(webLinkUrl).build()))
                    .addButton(new ButtonObject(appLinkText, LinkObject.newBuilder().setWebUrl(appLinkUrl).setMobileWebUrl(appLinkUrl).setAndroidExecutionParams("key1=value1").setIosExecutionParams("key1=value1").build()))
                    .build();

            KakaoLinkService.getInstance().sendDefault(getCurrentActivity(), params, new ResponseCallback<KakaoLinkResponse>() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    Log.v(LOG_TAG, "kakao link : error - " + errorResult.toString());
                }

                @Override
                public void onSuccess(KakaoLinkResponse result) {
                    Log.v(LOG_TAG, "kakao link : onSuccess - " + result.toString());
                }
            });            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Log in
     */
    private void login() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Session.getCurrentSession().open(AuthType.KAKAO_TALK, cordova.getActivity());
            }
        });
    }

    /**
     * Log out
     *
     * @param callbackContext
     */
    private void logout(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Log.v(LOG_TAG, "kakao : onCompleteLogout");
                        callbackContext.success();
                    }
                });
            }
        });
    }

    /**
     * 액세스 토큰을 가져온다
     */
    private void getAccessToken(CallbackContext callbackContext) {
        // this.login();
        String accessToken = Session.getCurrentSession().getTokenInfo().getAccessToken();
        callbackContext.success(accessToken);
    }

    /**
     * On activity result
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(LOG_TAG, "kakao : onActivityResult : " + requestCode + ", code: " + resultCode);
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, intent)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * Result
     *
     * @param userProfile
     */
    private JSONObject handleResult(UserProfile userProfile) {
        Log.v(LOG_TAG, "kakao : handleResult");
        JSONObject response = new JSONObject();
        try {
            Log.v(LOG_TAG, "kakao response: " + response);
            response.put("id", userProfile.getId());
            response.put("nickname", userProfile.getNickname());
            response.put("profile_image", userProfile.getProfileImagePath());
            response.put("email", userProfile.getEmail());
        } catch (JSONException e) {
            Log.v(LOG_TAG, "kakao : handleResult error - " + e.toString());
        }
        return response;
    }


    /**
     * Class SessonCallback
     */
    private class SessionCallback implements ISessionCallback {

        private CallbackContext callbackContext;

        public SessionCallback(final CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        @Override
        public void onSessionOpened() {
            Log.v(LOG_TAG, "kakao : SessionCallback.onSessionOpened");
            // List<String> propertyKeys = new ArrayList<String>();
            // propertyKeys.add("kaccount_email");
            // propertyKeys.add("nickname");
            // propertyKeys.add("profile_image");
            // propertyKeys.add("thumbnail_image");
            UserManagement.getInstance().requestMe(new MeResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    callbackContext.error("kakao : SessionCallback.onSessionOpened.requestMe.onFailure - " + errorResult);
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.v(LOG_TAG, "kakao : SessionCallback.onSessionOpened.requestMe.onSessionClosed - " + errorResult);
                    Session.getCurrentSession().checkAndImplicitOpen();
                }

                @Override
                public void onSuccess(UserProfile userProfile) {
                    // callbackContext.success(handleResult(userProfile));
                    getAccessToken(callbackContext);
                }

                @Override
                public void onNotSignedUp() {
                    callbackContext.error("this user is not signed up");
                }
            // }, propertyKeys, false);
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Log.v(LOG_TAG, "kakao : onSessionOpenFailed" + exception.toString());
            }
        }
    }


    /**
     * Return current activity
     */
    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * Set current activity
     */
    public static void setCurrentActivity(Activity currentActivity) {
        currentActivity = currentActivity;
    }

    /**
     * Class KakaoSDKAdapter
     */
    private static class KakaoSDKAdapter extends KakaoAdapter {

        @Override
        public ISessionConfig getSessionConfig() {
            return new ISessionConfig() {
                @Override
                public AuthType[] getAuthTypes() {
                    return new AuthType[]{AuthType.KAKAO_LOGIN_ALL};
                }

                @Override
                public boolean isUsingWebviewTimer() {
                    return false;
                }

                @Override
                public ApprovalType getApprovalType() {
                    return ApprovalType.INDIVIDUAL;
                }

                @Override
                public boolean isSecureMode() {
                    return false;
                }

                @Override
                public boolean isSaveFormData() {
                    return true;
                }
            };
        }

        @Override
        public IApplicationConfig getApplicationConfig() {
            return new IApplicationConfig() {
                @Override
                public Context getApplicationContext() {
                    return KakaoTalk.getCurrentActivity().getApplicationContext();
                }
            };
        }
    }

}
