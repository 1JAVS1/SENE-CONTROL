package com.sene.conect;
// UI neón SENE CONECT

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public class MainActivity extends Activity {
 private static final UUID SERVICE=UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"), RX=UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), TX=UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"), CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 private static final int GREEN=Color.rgb(25,151,126), NAVY=Color.rgb(242,246,249), INK=Color.rgb(31,48,67), MUTED=Color.rgb(112,130,146), CYAN=Color.rgb(67,112,170);
 private BluetoothAdapter adapter; private BluetoothLeScanner scanner; private BluetoothGatt gatt; private BluetoothGattCharacteristic writeChar;
 private final Handler handler=new Handler(Looper.getMainLooper()); private final ArrayDeque<byte[]> txQueue=new ArrayDeque<>(); private boolean txBusy=false; private final Map<String,BluetoothDevice> found=new LinkedHashMap<>(); private final ArrayList<String> routes=new ArrayList<>(); private final HashSet<String> selectedRoutes=new HashSet<>();
 private SharedPreferences prefs; private LinearLayout devices,routesBox; private TextView connection,connectionDot,message,count,scanMessage; private EditText input;  private boolean scanning=false,deviceWindow=false;

 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);prefs=getSharedPreferences("sene",MODE_PRIVATE);loadRoutes();BluetoothManager m=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);adapter=m==null?null:m.getAdapter();render();if(adapter==null)status("Este celular no tiene Bluetooth BLE.");else if(!adapter.isEnabled())status("Activa Bluetooth en el celular.");}
 private void permissions(){ArrayList<String> p=new ArrayList<>();if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=0)p.add(Manifest.permission.BLUETOOTH_SCAN);if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=0)p.add(Manifest.permission.BLUETOOTH_CONNECT);}else if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=0)p.add(Manifest.permission.ACCESS_FINE_LOCATION);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),41);}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private GradientDrawable shape(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
 private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
 private LinearLayout.LayoutParams lp(int w,int h,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,dp(top),0,dp(bottom));return p;}
 private TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private Button btn(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextColor(fg);b.setTextSize(12);b.setTypeface(null,Typeface.BOLD);b.setAllCaps(false);b.setMinHeight(dp(48));b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setElevation(dp(1));b.setBackground(shape(bg,15));return b;}
 private FrameLayout screenRoot(int theme){FrameLayout frame=new FrameLayout(this);frame.addView(new SubmenuBackdropView(theme),new FrameLayout.LayoutParams(-1,-1));ImageView watermark=new ImageView(this);Bitmap original=BitmapFactory.decodeResource(getResources(),R.drawable.sene_logo);Bitmap smoothLogo=Bitmap.createScaledBitmap(original,1024,640,true);watermark.setImageBitmap(smoothLogo);if(original!=smoothLogo)original.recycle();watermark.setScaleType(ImageView.ScaleType.FIT_CENTER);watermark.setAlpha(0.045f);FrameLayout.LayoutParams wm=new FrameLayout.LayoutParams(dp(320),dp(200),Gravity.CENTER);frame.addView(watermark,wm);return frame;}
 private class SubmenuBackdropView extends View{
  private final Paint p=new Paint(3);private final int theme;
  SubmenuBackdropView(int t){super(MainActivity.this);theme=t;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
   int a,b,glow;
   if(theme==1){a=Color.rgb(3,19,39);b=Color.rgb(4,47,59);glow=Color.rgb(0,220,165);}
   else if(theme==2){a=Color.rgb(3,12,42);b=Color.rgb(10,30,78);glow=Color.rgb(0,145,255);}
   else if(theme==3){a=Color.rgb(18,12,48);b=Color.rgb(5,34,67);glow=Color.rgb(157,93,255);}
   else if(theme==4){a=Color.rgb(3,30,35);b=Color.rgb(4,55,51);glow=Color.rgb(51,255,150);}
   else{a=Color.rgb(5,17,43);b=Color.rgb(18,30,65);glow=Color.rgb(0,180,255);}
   p.setShader(new LinearGradient(0,0,w,h,a,b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
   p.setColor(Color.argb(34,glow==0?0:Color.red(glow),Color.green(glow),Color.blue(glow)));c.drawCircle(w*.92f,h*.12f,Math.max(w,h)*.42f,p);
   p.setColor(Color.argb(24,Color.red(glow),Color.green(glow),Color.blue(glow)));c.drawCircle(w*.05f,h*.88f,Math.max(w,h)*.34f,p);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(Color.argb(24,160,220,255));
   float step=dp(28);for(float x=0;x<w;x+=step)c.drawLine(x,0,x,h,p);for(float y=0;y<h;y+=step)c.drawLine(0,y,w,y,p);
   p.setColor(Color.argb(70,Color.red(glow),Color.green(glow),Color.blue(glow)));p.setStrokeWidth(dp(1.5f));
   for(int i=0;i<5;i++){float r=dp(55+i*27);c.drawCircle(w*(theme==3?.82f:.92f),h*(theme==3?.19f:.12f),r,p);}
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));Path wave=new Path();wave.moveTo(-w*.1f,h*.68f);wave.cubicTo(w*.2f,h*.42f,w*.36f,h*.9f,w*.62f,h*.62f);wave.cubicTo(w*.82f,h*.4f,w*.94f,h*.78f,w*1.1f,h*.52f);p.setColor(Color.argb(105,Color.red(glow),Color.green(glow),Color.blue(glow)));c.drawPath(wave,p);
   p.setStyle(Paint.Style.FILL);
  }
 }
 private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(18),dp(18),dp(18),dp(18));c.setBackground(shape(Color.WHITE,22));c.setElevation(dp(2));return c;}
 private void title(LinearLayout p,String a,String b){p.addView(txt(a,16,INK,true),lp(-1,-2,0,0,0,3));p.addView(txt(b,12,MUTED,false),lp(-1,-2,0,0,0,9));}

 // Menú principal SENE CONECT: diseño neón personalizado.
 private void render(){
  deviceWindow=false;
  getWindow().setStatusBarColor(Color.rgb(2,11,42));
  getWindow().setNavigationBarColor(Color.rgb(2,11,42));
  FrameLayout base=new FrameLayout(this);
  base.setBackgroundColor(Color.rgb(2,11,42));
  base.addView(new NeonWavesView(),new FrameLayout.LayoutParams(-1,-1));
  LinearLayout page=new LinearLayout(this);page.setOrientation(1);
  page.setPadding(dp(18),dp(10),dp(18),dp(8));
  page.setBackgroundColor(Color.TRANSPARENT);
  base.addView(page,new FrameLayout.LayoutParams(-1,-1));

  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  BrandLogoView brand=new BrandLogoView();
  header.addView(brand,new LinearLayout.LayoutParams(0,dp(112),1));
  BluetoothBadgeView bluetooth=new BluetoothBadgeView();
  header.addView(bluetooth,new LinearLayout.LayoutParams(dp(150),dp(112)));
  page.addView(header,lp(-1,dp(112),0,2,0,dp(8)));

  TextView section=txt("MENÚ PRINCIPAL",11,Color.rgb(150,207,235),true);
  section.setLetterSpacing(.18f);
  page.addView(section,lp(-1,-2,0,0,0,dp(10)));

  ScrollView sc=new ScrollView(this);sc.setFillViewport(false);sc.setClipToPadding(false);
  sc.setVerticalScrollBarEnabled(false);
  LinearLayout list=new LinearLayout(this);list.setOrientation(1);list.setClipChildren(false);list.setClipToPadding(false);
  sc.addView(list,new ScrollView.LayoutParams(-1,-2));
  addHomeCard(list,0,"MIS DISPOSITIVOS","Gestiona tus equipos",()->{showDeviceWindow();if(hasScanPermissions())startScan();else permissions();});
  addHomeCard(list,1,"MIS RUTAS","Consulta y guarda tus rutas",()->renderMyRoutes());
  addHomeCard(list,2,"TRANSMISIÓN","Administra y envía rutas a tu guía",()->renderWorkspace());
  addHomeCard(list,3,"CONFIGURACIÓN","Ajusta las opciones",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));}catch(Exception e){status("No se pudieron abrir los ajustes Bluetooth.");}});
  addHomeCard(list,4,"ACERCA DE","Información de la app",()->new android.app.AlertDialog.Builder(this).setTitle("SENE CONECT").setMessage("Control de guías SENE mediante Bluetooth BLE.\\n\\nAdministra tus dispositivos y transmite rutas guardadas.").setPositiveButton("ACEPTAR",null).show());
  page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

  LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);
  View leftGreen=new View(this);leftGreen.setBackgroundColor(Color.rgb(51,255,0));
  View leftBlue=new View(this);leftBlue.setBackgroundColor(Color.rgb(13,95,255));
  TextView footerText=txt("SENE",17,Color.WHITE,true);footerText.setTypeface(Typeface.create("sans-serif",Typeface.BOLD_ITALIC));footerText.setGravity(Gravity.CENTER);
  View rightGreen=new View(this);rightGreen.setBackgroundColor(Color.rgb(51,255,0));
  View rightBlue=new View(this);rightBlue.setBackgroundColor(Color.rgb(13,95,255));
  LinearLayout leftLines=new LinearLayout(this);leftLines.setOrientation(1);leftLines.addView(leftGreen,new LinearLayout.LayoutParams(-1,dp(2)));leftLines.addView(leftBlue,lp(-1,dp(2),dp(4),0));
  LinearLayout rightLines=new LinearLayout(this);rightLines.setOrientation(1);rightLines.addView(rightGreen,new LinearLayout.LayoutParams(-1,dp(2)));rightLines.addView(rightBlue,lp(-1,dp(2),dp(4),0));
  footer.addView(leftLines,new LinearLayout.LayoutParams(0,dp(10),1));footer.addView(footerText,new LinearLayout.LayoutParams(dp(74),-2));footer.addView(rightLines,new LinearLayout.LayoutParams(0,dp(10),1));
  page.addView(footer,lp(-1,dp(26),dp(6),0));
  setContentView(base);
 }

 private void addHomeCard(LinearLayout parent,int icon,String heading,String detail,Runnable action){
  LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);
  card.setOrientation(0);card.setPadding(dp(12),dp(9),dp(12),dp(9));
  GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
   new int[]{Color.rgb(13,95,255),Color.rgb(18,160,150),Color.rgb(51,255,0)});
  bg.setCornerRadius(dp(24));bg.setStroke(dp(1),Color.argb(210,170,255,205));
  card.setBackground(bg);card.setElevation(dp(9));
  card.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
  MenuIconView glyph=new MenuIconView(icon);
  card.addView(glyph,new LinearLayout.LayoutParams(dp(54),dp(54)));
  View separator=new View(this);separator.setBackgroundColor(Color.argb(190,230,255,255));
  LinearLayout.LayoutParams sepLp=new LinearLayout.LayoutParams(dp(1),dp(42));sepLp.setMargins(dp(9),0,dp(12),0);card.addView(separator,sepLp);
  LinearLayout labels=new LinearLayout(this);labels.setOrientation(1);labels.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=txt(heading,15,Color.WHITE,true);title.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
  TextView subtitle=txt(detail,11,Color.rgb(235,245,245),false);
  labels.addView(title);labels.addView(subtitle,lp(-1,-2,dp(3),0));
  card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
  TextView arrow=txt("›",34,Color.WHITE,true);arrow.setGravity(Gravity.CENTER);
  card.addView(arrow,new LinearLayout.LayoutParams(dp(28),-1));
  card.setOnClickListener(v->action.run());
  parent.addView(card,lp(-1,dp(76),0,0,0,dp(12)));
 }

 private class NeonWavesView extends View {
  private final Paint p=new Paint(3);
  NeonWavesView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);float w=getWidth(),h=getHeight();
   p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.rgb(2,11,42),Color.rgb(3,23,57),Color.rgb(1,13,38)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
   for(int i=0;i<7;i++){
    float y=h*(.23f+i*.105f);
    Path path=new Path();path.moveTo(-w*.1f,y);
    path.cubicTo(w*.18f,y-h*.22f,w*.34f,y+h*.24f,w*.58f,y-h*.03f);
    path.cubicTo(w*.78f,y-h*.25f,w*.9f,y+h*.15f,w*1.12f,y-h*.08f);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(i%2==0?dp(2.2f):dp(1.2f));
    p.setColor(i%2==0?Color.rgb(0,145,255):Color.rgb(51,255,0));
    p.setAlpha(i%2==0?105:80);p.setMaskFilter(new BlurMaskFilter(dp(i%2==0?10:7),BlurMaskFilter.Blur.OUTER));c.drawPath(path,p);
    p.setMaskFilter(null);p.setAlpha(i%2==0?170:135);p.setStrokeWidth(dp(1));c.drawPath(path,p);
   }
   p.setStyle(Paint.Style.FILL);p.setAlpha(255);
   p.setColor(Color.argb(24,0,220,255));c.drawCircle(w*.08f,h*.75f,dp(95),p);
   p.setColor(Color.argb(18,51,255,0));c.drawCircle(w*.94f,h*.28f,dp(105),p);
  }
 }

 private class BrandLogoView extends View {
  private final Paint p=new Paint(3);
  BrandLogoView(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);float w=getWidth(),h=getHeight();
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(51,255,0));p.setShadowLayer(dp(12),0,0,Color.rgb(51,255,0));
   RectF oval=new RectF(dp(3),dp(10),w-dp(8),h-dp(13));c.drawArc(oval,205,305,false,p);p.clearShadowLayer();
   p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
   p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD_ITALIC));p.setTextSize(dp(36));
   c.drawText("SENE",w*.5f,h*.55f,p);
   p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(dp(15));
   c.drawText("CONECT",w*.5f,h*.77f,p);
   p.setTextAlign(Paint.Align.LEFT);
  }
 }

 private class BluetoothBadgeView extends View {
  private final Paint p=new Paint(3);
  BluetoothBadgeView(){super(MainActivity.this);}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);float cx=getWidth()/2f,cy=dp(38),r=dp(28);
   p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(0,85,210));p.setShadowLayer(dp(15),0,0,Color.rgb(0,125,255));c.drawCircle(cx,cy,r,p);p.clearShadowLayer();
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.8f));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(Color.WHITE);
   Path b=new Path();b.moveTo(cx,cy-dp(17));b.lineTo(cx,cy+dp(17));b.moveTo(cx,cy-dp(17));b.lineTo(cx+dp(11),cy-dp(7));b.lineTo(cx-dp(11),cy+dp(9));b.moveTo(cx,cy+dp(17));b.lineTo(cx+dp(11),cy+dp(7));b.lineTo(cx-dp(11),cy-dp(9));c.drawPath(b,p);
   p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,255,0));c.drawCircle(cx+dp(22),cy+dp(19),dp(9),p);
   p.setColor(Color.rgb(0,40,30));p.setStrokeWidth(dp(2));p.setStyle(Paint.Style.STROKE);Path check=new Path();check.moveTo(cx+dp(17),cy+dp(19));check.lineTo(cx+dp(21),cy+dp(23));check.lineTo(cx+dp(27),cy+dp(15));c.drawPath(check,p);
   p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,255,0));p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(dp(8));String s="LISTO PARA RECIBIR";c.drawText(s,cx-p.measureText(s)/2,dp(82),p);String s2="DESDE SENE.CONECT";c.drawText(s2,cx-p.measureText(s2)/2,dp(93),p);
  }
 }

 private class MenuIconView extends View {
  private final int kind;private final Paint p=new Paint(3);
  MenuIconView(int k){super(MainActivity.this);kind=k;}
  private void line(Canvas c,float... xy){Path q=new Path();q.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)q.lineTo(xy[i],xy[i+1]);c.drawPath(q,p);}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);c.save();c.translate(getWidth()/2f,getHeight()/2f);float u=dp(1);
   p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
   if(kind==0){c.drawRoundRect(-dp(19),-dp(15),dp(15),dp(10),dp(3),dp(3),p);line(c,-dp(8),dp(15),dp(5),dp(15),dp(1),dp(10));c.drawArc(-dp(13),-dp(23),dp(11),dp(1),215,110,false,p);c.drawArc(-dp(8),-dp(18),dp(6),-dp(4),220,100,false,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,255,0));c.drawCircle(dp(18),-dp(15),dp(3),p);}
   else if(kind==1){Path m=new Path();m.moveTo(-dp(19),-dp(13));m.lineTo(-dp(3),-dp(18));m.lineTo(dp(5),-dp(13));m.lineTo(dp(19),-dp(17));m.lineTo(dp(19),dp(12));m.lineTo(dp(5),dp(17));m.lineTo(-dp(3),dp(12));m.lineTo(-dp(19),dp(16));m.close();c.drawPath(m,p);line(c,-dp(3),-dp(18),-dp(3),dp(12));line(c,dp(5),-dp(13),dp(5),dp(17));p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,255,0));c.drawCircle(dp(5),-dp(4),dp(5),p);}
   else if(kind==2){c.drawCircle(0,-dp(9),dp(4),p);line(c,0,-dp(5),0,dp(16));line(c,-dp(12),dp(16),dp(12),dp(16));c.drawArc(-dp(15),-dp(24),dp(15),dp(6),205,130,false,p);c.drawArc(-dp(22),-dp(30),dp(22),dp(12),205,130,false,p);}
   else if(kind==3){c.drawCircle(0,0,dp(8),p);for(int i=0;i<8;i++){c.save();c.rotate(i*45);c.drawRoundRect(-dp(3),-dp(22),dp(3),-dp(14),dp(2),dp(2),p);c.restore();}p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(51,255,0));c.drawCircle(0,0,dp(3),p);}
   else {c.drawCircle(0,0,dp(18),p);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(dp(25));p.setColor(Color.WHITE);c.drawText("i",-dp(3.5f),dp(9),p);}
   c.restore();
  }
 }
 private void renderMyRoutes(){
  deviceWindow=false;FrameLayout base=screenRoot(1);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(16),dp(18),dp(28));scroll.addView(page);base.addView(scroll,new FrameLayout.LayoutParams(-1,-1));setContentView(base);
  TextView back=txt("‹  MENÚ PRINCIPAL",13,Color.WHITE,true);back.setPadding(dp(4),dp(2),0,dp(10));back.setOnClickListener(v->render());page.addView(back,lp(-1,-2,0,0,0,8));
  TextView heading=txt("MIS RUTAS",24,Color.WHITE,true);page.addView(heading,lp(-1,-2,0,8,0,3));page.addView(txt("Consulta tus destinos guardados y agrega nuevas rutas.",12,Color.rgb(190,215,235),false),lp(-1,-2,0,0,0,14));
  LinearLayout rc=card();count=txt("RUTAS GUARDADAS  ·  "+routes.size()+" / 20",11,CYAN,true);rc.addView(count,lp(-1,-2,0,0,0,10));routesBox=new LinearLayout(this);routesBox.setOrientation(1);rc.addView(routesBox);renderRoutes();
  TextView hint=txt("ESCRIBIR NUEVAS RUTAS",11,CYAN,true);rc.addView(hint,lp(-1,-2,0,14,0,6));
  input=new EditText(this);input.setHint("Separa las rutas con puntos. Ejemplo: CENTRO.PLAZA.HOSPITAL");input.setTextColor(INK);input.setHintTextColor(MUTED);input.setGravity(Gravity.TOP|Gravity.START);input.setTextSize(14);input.setMinLines(2);input.setPadding(dp(14),dp(13),dp(14),dp(13));input.setBackground(shape(Color.rgb(244,246,251),14));rc.addView(input,lp(-1,-2,0,0,0,10));
  Button save=btn("＋  GUARDAR RUTAS",INK,Color.WHITE);save.setOnClickListener(v->addRoutes());rc.addView(save,lp(-1,dp(50),0,0,0,0));page.addView(rc,lp(-1,-2,0,0,0,16));
 }
 private void renderWorkspace(){
  deviceWindow=false;FrameLayout base=screenRoot(2);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(16),dp(18),dp(28));scroll.addView(page);base.addView(scroll,new FrameLayout.LayoutParams(-1,-1));setContentView(base);
  TextView back=txt("‹  MENÚ PRINCIPAL",13,Color.WHITE,true);back.setPadding(dp(4),dp(2),0,dp(10));back.setOnClickListener(v->render());page.addView(back,lp(-1,-2,0,0,0,8));
  TextView heading=txt("TRANSMISIÓN",24,Color.WHITE,true);page.addView(heading,lp(-1,-2,0,8,0,3));page.addView(txt("Selecciona las rutas que recibirá tu guía SENE.",12,Color.rgb(190,215,235),false),lp(-1,-2,0,0,0,14));
  LinearLayout ready=card();ready.setOrientation(1);ready.addView(txt("ESTADO DEL CONTROLADOR",10,MUTED,true));connection=txt(gatt!=null&&writeChar!=null?"CONECTADO · LISTO PARA ENVIAR":"SIN CONEXIÓN · GUÍA SENE",15,gatt!=null&&writeChar!=null?GREEN:Color.rgb(204,75,83),true);ready.addView(connection,lp(-1,-2,0,5,0,0));Button scan=btn(gatt!=null&&writeChar!=null?"ADMINISTRAR CONEXIÓN":"＋  CONECTAR DISPOSITIVO SENE",CYAN,Color.WHITE);scan.setOnClickListener(v->{showDeviceWindow();if(hasScanPermissions())startScan();else permissions();});ready.addView(scan,lp(-1,dp(50),12,0,0,0));page.addView(ready,lp(-1,-2,0,0,0,14));
  LinearLayout rc=card();rc.addView(txt("RUTAS DISPONIBLES",15,INK,true),lp(-1,-2,0,0,0,4));count=txt("RUTAS GUARDADAS  ·  "+routes.size()+" / 20",11,CYAN,true);rc.addView(count,lp(-1,-2,0,0,0,10));routesBox=new LinearLayout(this);routesBox.setOrientation(1);rc.addView(routesBox);renderTransmissionRoutes();
  Button manage=btn("☷  ADMINISTRAR Y SELECCIONAR RUTAS",Color.rgb(232,239,247),CYAN);manage.setOnClickListener(v->showRouteManager());rc.addView(manage,lp(-1,dp(48),12,0,0,0));Button send=btn("▶  TRANSMITIR RUTAS SELECCIONADAS",GREEN,Color.WHITE);send.setOnClickListener(v->sendRoutes());rc.addView(send,lp(-1,dp(54),9,0,0,0));page.addView(rc,lp(-1,-2,0,0,0,14));
  message=txt("Conecta tu guía SENE y selecciona las rutas que deseas enviar.",12,Color.rgb(190,215,235),false);message.setGravity(Gravity.CENTER);page.addView(message,lp(-1,-2,4,0));
 }
 private void renderTransmissionRoutes(){if(routesBox==null)return;routesBox.removeAllViews();if(routes.isEmpty()){routesBox.addView(txt("No hay rutas guardadas. Agrégalas en MIS RUTAS.",13,MUTED,false));return;}for(String route:routes){CheckBox check=new CheckBox(this);check.setText(route);check.setTextColor(INK);check.setTextSize(14);check.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));check.setChecked(selectedRoutes.contains(route));check.setOnCheckedChangeListener((v,on)->{if(on)selectedRoutes.add(route);else selectedRoutes.remove(route);});routesBox.addView(check,lp(-1,-2,2,2,0,2));}}
 private void showDeviceWindow(){
  deviceWindow=true;FrameLayout base=screenRoot(3);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(20),dp(18),dp(22));root.setBackgroundColor(Color.TRANSPARENT);base.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(base);
  TextView back=txt("‹  CONEXIÓN BLUETOOTH",14,CYAN,true);root.addView(back,lp(-1,dp(42),0,8));back.setOnClickListener(v->{stopScan();render();});
  LinearLayout panel=card();panel.setGravity(Gravity.CENTER);panel.addView(txt("AGREGAR DISPOSITIVO",22,INK,true),lp(-1,-2,0,8));panel.addView(txt("Seleccione SENE para enlazar con su guía SENE.",14,MUTED,false),lp(-1,-2,0,14));scanMessage=txt(scanning?"Buscando dispositivos SENE…":"Pulsa buscar para localizar tu controlador.",13,MUTED,false);scanMessage.setGravity(Gravity.CENTER);panel.addView(scanMessage,lp(-1,-2,8,12));devices=new LinearLayout(this);devices.setOrientation(1);panel.addView(devices);Button search=btn("⌕  BUSCAR SENE",CYAN,Color.WHITE);panel.addView(search,lp(-1,dp(50),12,0));search.setOnClickListener(v->{if(hasScanPermissions())startScan();else permissions();});root.addView(panel,lp(-1,-2,0,12));renderDevices();
 }
 private void showLinkedScreen(){
  deviceWindow=true;stopScan();FrameLayout base=screenRoot(4);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setGravity(Gravity.CENTER);root.setPadding(dp(22),dp(28),dp(22),dp(28));root.setBackgroundColor(Color.TRANSPARENT);base.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(base);
  TextView check=txt("✓",60,GREEN,true);check.setGravity(Gravity.CENTER);root.addView(check,lp(-1,dp(88),0,8));TextView name=txt("SENE",30,Color.WHITE,true);name.setGravity(Gravity.CENTER);root.addView(name,lp(-1,-2,0,5));TextView linked=txt("ENLAZADO",21,GREEN,true);linked.setGravity(Gravity.CENTER);root.addView(linked,lp(-1,-2,0,8));TextView note=txt("El controlador está conectado y listo para recibir rutas.",15,Color.rgb(220,240,235),false);note.setGravity(Gravity.CENTER);root.addView(note,lp(-1,-2,0,24));Button back=btn("REGRESAR AL MENÚ PRINCIPAL",GREEN,Color.WHITE);root.addView(back,lp(-1,dp(54),24,0));back.setOnClickListener(v->render());
 }
 private void status(String s){if(message!=null)message.setText(s);boolean linked=gatt!=null&&writeChar!=null;if(connection!=null){connection.setText(linked?"LISTO PARA ENVIAR DESDE SENE.CONECT":"NO CONECTADO CON GUÍA SENE");connection.setTextColor(linked?GREEN:Color.RED);}if(connectionDot!=null)connectionDot.setTextColor(linked?GREEN:Color.RED);if(deviceWindow&&scanMessage!=null)scanMessage.setText(s);}
 private boolean hasScanPermissions(){if(Build.VERSION.SDK_INT>=31)return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;return Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 private void startScan(){if(adapter==null){status("Este celular no tiene Bluetooth BLE.");return;}if(!adapter.isEnabled()){status("Activa Bluetooth en el celular.");return;}if(!hasScanPermissions()){ArrayList<String> p=new ArrayList<>();if(Build.VERSION.SDK_INT>=31){p.add(Manifest.permission.BLUETOOTH_SCAN);p.add(Manifest.permission.BLUETOOTH_CONNECT);}else if(Build.VERSION.SDK_INT>=23)p.add(Manifest.permission.ACCESS_FINE_LOCATION);requestPermissions(p.toArray(new String[0]),41);status("Autoriza Bluetooth para buscar tu controlador.");return;}if(scanning){stopScan();return;}if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(SecurityException ignored){}gatt=null;writeChar=null;}found.clear();renderDevices();try{scanner=adapter.getBluetoothLeScanner();if(scanner==null){status("Bluetooth BLE no está disponible. Activa Bluetooth y vuelve a intentar.");return;}scanner.startScan(callback);scanning=true;status("Buscando dispositivos SENE...");handler.removeCallbacksAndMessages(null);handler.postDelayed(this::stopScan,15000);}catch(SecurityException e){status("Android bloqueó el escaneo. Revisa el permiso Dispositivos cercanos.");}catch(IllegalStateException e){status("No se pudo iniciar el escaneo. Intenta nuevamente.");}}
 private final ScanCallback callback=new ScanCallback(){@Override public void onScanResult(int t,ScanResult r){BluetoothDevice d=r.getDevice();String n=d.getName();if((n==null||n.isEmpty())&&r.getScanRecord()!=null)n=r.getScanRecord().getDeviceName();if(n==null)n="";if(n.toUpperCase(Locale.ROOT).contains("SENE")){found.put(d.getAddress(),d);runOnUiThread(()->{renderDevices();status("Controlador SENE encontrado. Tócalo para enlazar.");});}}@Override public void onScanFailed(int e){runOnUiThread(()->{scanning=false;status("Falló el escaneo BLE (código "+e+"). Activa Bluetooth e intenta otra vez.");});}};
 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==41){if(hasScanPermissions())startScan();else status("Sin permiso de Dispositivos cercanos no se puede buscar SENE.");}}
 private void stopScan(){if(scanner!=null&&scanning){try{scanner.stopScan(callback);}catch(SecurityException ignored){}scanning=false;}if(found.isEmpty())status("No se encontró SENE. Verifica que el controlador tenga Bluetooth activado desde su menú y vuelve a buscar.");else status("Selecciona el controlador SENE para enlazar.");}
 private void renderDevices(){if(devices==null)return;devices.removeAllViews();if(found.isEmpty()){TextView empty=txt("Aquí aparecerá SENE cuando sea detectado.",13,MUTED,false);empty.setGravity(Gravity.CENTER);devices.addView(empty,lp(-1,dp(58),0,0,0,0));return;}for(BluetoothDevice d:found.values()){Button item=btn("◉   SENE",Color.rgb(232,240,247),INK);item.setTextSize(17);devices.addView(item,lp(-1,dp(56),5,5,0,0));item.setOnClickListener(v->connect(d));}}
 private void connect(BluetoothDevice d){stopScan();status("Enlazando con SENE…");if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(Exception ignored){}gatt=null;}writeChar=null;try{gatt=d.connectGatt(this,false,gattCallback,BluetoothDevice.TRANSPORT_LE);}catch(SecurityException e){status("Permiso Bluetooth requerido.");}}
 private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
  @Override public void onConnectionStateChange(BluetoothGatt g,int st,int state){if(st!=BluetoothGatt.GATT_SUCCESS){runOnUiThread(()->status("No se pudo enlazar. Intenta de nuevo."));return;}if(state==BluetoothProfile.STATE_CONNECTED){runOnUiThread(()->status("Buscando servicios SENE…"));try{g.discoverServices();}catch(SecurityException ignored){}}else if(state==BluetoothProfile.STATE_DISCONNECTED){writeChar=null;runOnUiThread(()->{if(deviceWindow)showDeviceWindow();else render();status("Bluetooth desconectado.");});}}
  @Override public void onServicesDiscovered(BluetoothGatt g,int st){BluetoothGattService service=st==BluetoothGatt.GATT_SUCCESS?g.getService(SERVICE):null;writeChar=service==null?null:service.getCharacteristic(RX);BluetoothGattCharacteristic notify=service==null?null:service.getCharacteristic(TX);if(notify!=null)try{g.setCharacteristicNotification(notify,true);BluetoothGattDescriptor d=notify.getDescriptor(CCCD);if(d!=null){d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);g.writeDescriptor(d);}}catch(SecurityException ignored){}runOnUiThread(()->{if(writeChar==null)status("El dispositivo no ofrece el servicio SENE.");else{prefs.edit().putString("device",g.getDevice().getAddress()).apply();showLinkedScreen();}});}
  @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] b=c.getValue();if(b!=null)runOnUiThread(()->status(new String(b,StandardCharsets.UTF_8).trim()));}
  @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int st){runOnUiThread(()->{if(st!=BluetoothGatt.GATT_SUCCESS){txQueue.clear();txBusy=false;status("No se pudo enviar.");}else if(!txQueue.isEmpty())writeNext();else{txBusy=false;status("Rutas enviadas al controlador.");}});}
 };
 private String removeAccents(String value){return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","");}
 private void addRoutes(){String raw=removeAccents(input.getText().toString()).trim().toUpperCase(Locale.ROOT);if(raw.isEmpty()){status("Guarda al menos una ruta.");return;}for(String v:raw.split("[.\\n,;]+")){String r=removeAccents(v).trim().replaceAll("\\s+"," ");if(!r.isEmpty()&&!routes.contains(r)&&routes.size()<20){routes.add(r);selectedRoutes.add(r);}}saveRoutes();input.setText("");renderRoutes();status("Rutas guardadas sin acentos.");}
 private void showRouteManager(){deviceWindow=true;FrameLayout base=screenRoot(5);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(22));root.setBackgroundColor(Color.TRANSPARENT);base.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(base);TextView back=txt("‹  REGRESAR AL MENÚ PRINCIPAL",14,CYAN,true);root.addView(back,lp(-1,dp(44),0,4));back.setOnClickListener(v->render());TextView heading=txt("ADMINISTRAR RUTAS",23,Color.WHITE,true);root.addView(heading,lp(-1,-2,0,14));TextView sub=txt("SELECCIONE RUTAS A ENVIAR",13,Color.rgb(70,255,190),true);root.addView(sub,lp(-1,-2,0,6));root.addView(txt("Marca las rutas que deseas enviar a tu guía SENE.",13,Color.rgb(205,220,240),false),lp(-1,-2,0,12));message=txt("Selecciona las rutas que deseas enviar.",12,Color.rgb(205,220,240),false);root.addView(message,lp(-1,-2,0,8));ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));if(routes.isEmpty())list.addView(txt("No hay rutas guardadas.",14,MUTED,false),lp(-1,-2,0,10));for(String route:new ArrayList<>(routes)){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(7),dp(8),dp(7));row.setBackground(shape(Color.rgb(247,250,252),14));CheckBox check=new CheckBox(this);check.setText(route);check.setTextColor(INK);check.setTextSize(15);check.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));check.setChecked(selectedRoutes.contains(route));check.setOnCheckedChangeListener((v,on)->{if(on)selectedRoutes.add(route);else selectedRoutes.remove(route);});row.addView(check,new LinearLayout.LayoutParams(0,-2,1));Button del=btn("×",Color.rgb(255,235,235),Color.rgb(190,55,55));del.setMinWidth(dp(42));del.setOnClickListener(v->{routes.remove(route);selectedRoutes.remove(route);saveRoutes();showRouteManager();});row.addView(del);list.addView(row,lp(-1,-2,0,4,0,4));}Button all=btn("SELECCIONAR TODAS",CYAN,Color.WHITE);all.setOnClickListener(v->{selectedRoutes.clear();selectedRoutes.addAll(routes);showRouteManager();});root.addView(all,lp(-1,dp(44),10,0,0,0));Button none=btn("DESELECCIONAR TODAS",Color.rgb(232,239,247),CYAN);none.setOnClickListener(v->{selectedRoutes.clear();showRouteManager();});root.addView(none,lp(-1,dp(44),7,0,0,0));Button send=btn("▶  ENVIAR RUTAS SELECCIONADAS",GREEN,Color.WHITE);send.setOnClickListener(v->sendRoutes());root.addView(send,lp(-1,dp(52),10,0,0,0));}
 private void renderRoutes(){if(routesBox==null)return;routesBox.removeAllViews();count.setText("RUTAS GUARDADAS  ·  "+routes.size()+" / 20");if(routes.isEmpty()){routesBox.addView(txt("No hay rutas guardadas.",13,MUTED,false));return;}for(int i=0;i<routes.size();i++){final int index=i;LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(10),dp(9),dp(8),dp(9));r.setBackground(shape(Color.rgb(246,249,251),12));r.addView(txt(String.format(Locale.ROOT,"%02d   %s",i+1,routes.get(i)),14,INK,true),new LinearLayout.LayoutParams(0,-2,1));Button del=btn("×",Color.rgb(255,235,235),Color.rgb(190,55,55));del.setMinWidth(dp(42));del.setOnClickListener(v->{routes.remove(index);saveRoutes();renderRoutes();});r.addView(del);routesBox.addView(r,lp(-1,-2,0,3,0,3));}}
 private void loadRoutes(){String s=prefs.getString("routes","");if(!s.isEmpty())for(String r:s.split("\\n"))if(!r.trim().isEmpty()&&routes.size()<20){routes.add(removeAccents(r.trim()).toUpperCase(Locale.ROOT));selectedRoutes.add(routes.get(routes.size()-1));}}
 private void saveRoutes(){prefs.edit().putString("routes",String.join("\n",routes)).apply();}
 private void sendRoutes(){if(gatt==null||writeChar==null){status("Primero enlaza con SENE.");return;}ArrayList<String> toSend=new ArrayList<>();for(String route:routes)if(selectedRoutes.contains(route))toSend.add(route);if(toSend.isEmpty()){status("Selecciona al menos una ruta en Administrar rutas.");return;}if(txBusy){status("Espera a que termine el envío.");return;}long epoch=System.currentTimeMillis()/1000L;String timeMsg="TIME="+epoch+"\n";String routesMsg="AMANDAR"+String.join(".",toSend)+".\n";txQueue.clear();txQueue.add(timeMsg.getBytes(StandardCharsets.UTF_8));txQueue.add(routesMsg.getBytes(StandardCharsets.UTF_8));txBusy=true;writeNext();} private void writeNext(){if(gatt==null||writeChar==null){txQueue.clear();txBusy=false;status("Bluetooth desconectado.");return;}if(txQueue.isEmpty()){txBusy=false;status("Rutas enviadas al controlador.");return;}byte[] data=txQueue.remove();try{writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);writeChar.setValue(data);if(!gatt.writeCharacteristic(writeChar)){txQueue.clear();txBusy=false;status("No se pudo enviar.");}else status("Enviando rutas...");}catch(SecurityException e){txQueue.clear();txBusy=false;status("Permiso Bluetooth requerido.");}}
 @Override public void onBackPressed(){stopScan();render();}
 @Override protected void onDestroy(){stopScan();if(gatt!=null){try{gatt.close();}catch(Exception ignored){}gatt=null;}super.onDestroy();}
}
