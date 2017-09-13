package lanb.t1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    // 语音听写对象
    private SpeechRecognizer mIat;
    private EditText mResultText;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    // 语音合成对象
    private SpeechSynthesizer mTts;

    //UI
    private RecognizerDialog mIatDialog;

    //翻译
    public static final int SHOW_RESPONSE = 0;
    private TextView textView_response;
    //新建Handler的对象，在这里接收Message，然后更新TextView控件的内容
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_RESPONSE:
                    String response = (String) msg.obj;
                    textView_response.setText(response);
                    break;

                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpeechUtility.createUtility(this, SpeechConstant.APPID+"=597005ef");
        mResultText= (EditText) findViewById(R.id.etText);

        // 初始化听写对象
        mIat= SpeechRecognizer.createRecognizer(this, null);
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, null);

        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        //mIatDialog = new RecognizerDialog(this, null);

        //翻译
        textView_response = (TextView)findViewById(R.id.TextView1);
    }


    int ret=0;//听写状态，=0表示成功
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bBroad:
                if(mTts==null){
                    Toast.makeText(this, "HI", Toast.LENGTH_SHORT).show();
                }else {
                    //2.合成参数设置，详见《科大讯飞MSC API手册(Android)》SpeechSynthesizer 类
                    mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");//设置发音人  
                    mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速  
                    mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围0~100  
                    mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端

                    String str=mResultText.getText().toString();
                    mTts.startSpeaking(str, mSynListener);
                }
                break;
            case R.id.bSpeech:
                // mIat.startListening(mRecoListener)；
                mResultText.setText(null);//清空原有内容

                if(mIat==null) {//如果听写对象mIat没有被实例化
                    Toast.makeText(this, "HI", Toast.LENGTH_SHORT).show();
                }else{
                    //2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
                    mIat.setParameter(SpeechConstant.DOMAIN, "iat");
                    mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                    mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");

                    //显示窗口
                 //   mIatDialog.setListener(mRecognizerDialogListener);
                  //  mIatDialog.show();

                    ret = mIat.startListening(mRecoListener);
                    if (ret != ErrorCode.SUCCESS) {
                        Toast.makeText(this, "失败" + ret, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "成功" + ret, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btrans:
                sendRequestWithHttpClient();
                //Toast.makeText(this, "hhh", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    ////语音识别
    //3.开始听写   mIat.startListening(mRecoListener);
    //听写监听器
    private RecognizerListener mRecoListener = new RecognizerListener(){

        //听写结果回调接口(返回Json格式结果，用户可参见附录12.1)；
        //一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
        //关于解析Json的代码可参见MscDemo中JsonParser类；
        //isLast等于true时会话结束。
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.e("Result:",results.getResultString ());
            printResult(results);
        }
        //会话发生错误回调接口
        public void onError(SpeechError error) {
            error.getPlainDescription(true);//获取错误码描述
        }


        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        //开始录音
        public void onBeginOfSpeech() {
            Toast.makeText(MainActivity.this, "开始说话", Toast.LENGTH_SHORT).show();
        }
        //音量值0~30
        public void onVolumeChanged(int volume){}
        //结束录音
        public void onEndOfSpeech() {
            Toast.makeText(MainActivity.this, "结束说话", Toast.LENGTH_SHORT).show();
        }
        //扩展用接口
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {}
    };

    /**
     * 听写UI监听器
     */
    /*  private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

          //识别回调错误.

        public void onError(SpeechError error) {
            Toast.makeText(MainActivity.this, "error.getPlainDescription(true)", Toast.LENGTH_SHORT).show();
        }
    };*/


    //输出结果到界面EditText
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        mResultText.setText(resultBuffer.toString());
        mResultText.setSelection(mResultText.length());
    }

    private SynthesizerListener mSynListener = new SynthesizerListener() {
        //会话结束回调接口，没有错误时，error为null
        public void onCompleted(SpeechError error) {
        }

        //缓冲进度回调
        //percent为缓冲进度0~100，beginPos为缓冲音频在文本中开始位置，endPos表示缓冲音频在文本中结束位置，info为附加信息。
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
        }

        //开始播放
        public void onSpeakBegin() {
            Toast.makeText(MainActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
        }

        //暂停播放
        public void onSpeakPaused() {
        }

        //播放进度回调
        //percent为播放进度0~100,beginPos为播放音频在文本中开始位置，endPos表示播放音频在文本中结束位置.
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        //恢复播放回调接口
        public void onSpeakResumed() {
        }

        //会话事件回调接口
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
        }
    };



    //翻译
    private void sendRequestWithHttpClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "http://211.71.76.199:30000/translate/";
                // 创建HttpPost对象 
                HttpPost httpPost = new HttpPost(url);
                // 设置HTTP POST请求参数必须用NameValuePair对象
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                String gz=mResultText.getText().toString();
                params.add(new BasicNameValuePair("src_id", "ch"));
                params.add(new BasicNameValuePair("tgt_id", "jp"));
                params.add(new BasicNameValuePair("sentence", gz));
                //params.add(new BasicNameValuePair("sentence", "天气真好"));
                HttpResponse httpResponse = null;
                try {
                    // 设置httpPost请求参数 
                    httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                    httpResponse = new DefaultHttpClient().execute(httpPost);
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        // 使用getEntity方法活得返回结果
                        String result = EntityUtils.toString(httpResponse.getEntity());
                        //在子线程中将Message对象发出去
                        Message message = new Message();
                        message.what = SHOW_RESPONSE;
                        message.obj = result.toString();
                        handler.sendMessage(message);
                    }
                    else{
                        String result=gz+"翻译失败,错误代码："+httpResponse.getStatusLine().getStatusCode();
                        //在子线程中将Message对象发出去
                        Message message = new Message();
                        message.what = SHOW_RESPONSE;
                        message.obj = result.toString();
                        handler.sendMessage(message);
                    }
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
