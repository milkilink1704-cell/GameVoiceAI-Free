package com.gamevoice.free;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private SpeechRecognizer recognizer; private TextToSpeech tts;
 private boolean running=false, listening=false, busy=false;
 private TextView status, history; private EditText endpoint, apiKey; private Button toggle;
 private final int purple=Color.rgb(167,110,255);
 @Override public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(26,36,26,20);root.setBackgroundColor(Color.rgb(15,12,25));
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  TextView title=text("GAMEVOICE AI  FREE",26,purple);title.setTypeface(null,Typeface.BOLD);root.addView(title);
  root.addView(text("PS5 • English → Русский • динамик телефона",14,Color.LTGRAY));
  status=text("Ожидание запуска",17,Color.WHITE);root.addView(status);
  root.addView(text("HTTPS-адрес сервера LibreTranslate (с /translate):",14,Color.LTGRAY));
  endpoint=field("https://your-server.example/translate");endpoint.setText(getPreferences(0).getString("endpoint",""));root.addView(endpoint);
  root.addView(text("API-ключ (если требуется; необязательно):",14,Color.LTGRAY));
  apiKey=field("API key");apiKey.setText(getPreferences(0).getString("key",""));root.addView(apiKey);
  toggle=new Button(this);toggle.setText("НАЧАТЬ ПЕРЕВОД");root.addView(toggle);
  history=text("Здесь появятся реплики и перевод.",16,Color.WHITE);root.addView(history);
  root.addView(text("Прототип: нужен установленный движок распознавания речи и доступный сервер LibreTranslate. Не поддерживает перевод в фоне или при заблокированном экране.",13,Color.LTGRAY));
  tts=new TextToSpeech(this, result->{if(result==TextToSpeech.SUCCESS){int r=tts.setLanguage(new Locale("ru","RU"));if(r<0)status.setText("Установите русский голос TTS в настройках телефона");}});
  toggle.setOnClickListener(v->{if(running)stop();else start();});
 }
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,14,0,14);return t;}
 private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setHint(hint);return e;}
 private void start(){String url=endpoint.getText().toString().trim();if(!url.startsWith("https://")){status.setText("Укажите HTTPS-адрес сервера перевода");return;}
  getPreferences(0).edit().putString("endpoint",url).putString("key",apiKey.getText().toString()).apply();
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){status.setText("На телефоне не найден движок распознавания речи. Установите совместимый сервис.");return;}
  running=true;toggle.setText("ОСТАНОВИТЬ");status.setText("Слушаю английскую речь…");
  recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){listening=true;}public void onBeginningOfSpeech(){}public void onRmsChanged(float f){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){listening=false;}
   public void onError(int e){listening=false;if(running){status.setText("Повтор прослушивания (код "+e+")");restart(900);}}
   public void onResults(Bundle b){listening=false;ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())translate(a.get(0));restart(350);}
   public void onPartialResults(Bundle b){}public void onEvent(int type,Bundle b){}
  });listen();
 }
 private void listen(){if(!running||recognizer==null||listening)return;try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);recognizer.startListening(i);}catch(Exception e){status.setText("Ошибка микрофона: "+e.getMessage());restart(1500);}}
 private void restart(long delay){ui.postDelayed(()->{if(running)listen();},delay);}
 private void translate(String english){if(busy)return;busy=true;String url=endpoint.getText().toString().trim(),key=apiKey.getText().toString().trim();status.setText("Перевожу…");worker.execute(()->{
  String russian;try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(7000);c.setReadTimeout(12000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
   JSONObject body=new JSONObject();body.put("q",english);body.put("source","en");body.put("target","ru");body.put("format","text");if(!key.isEmpty())body.put("api_key",key);
   try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
   if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
   try(InputStream is=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))!=-1)out.write(buf,0,n);russian=new JSONObject(out.toString("UTF-8")).getString("translatedText");}c.disconnect();
  }catch(Exception ex){russian="Ошибка перевода: "+ex.getMessage();}
  String result=russian;ui.post(()->{busy=false;history.setText("EN: "+english+"\n\nRU: "+result+"\n\n"+history.getText());status.setText(running?"Слушаю английскую речь…":"Остановлено");if(!result.startsWith("Ошибка")&&tts!=null)tts.speak(result,TextToSpeech.QUEUE_ADD,null,"game-"+System.nanoTime());});
 });}
 private void stop(){running=false;listening=false;toggle.setText("НАЧАТЬ ПЕРЕВОД");status.setText("Остановлено");if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}if(tts!=null)tts.stop();}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==1&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)start();else status.setText("Нужно разрешение на микрофон");}
 @Override protected void onDestroy(){stop();if(tts!=null)tts.shutdown();worker.shutdownNow();super.onDestroy();}
}
