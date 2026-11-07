import linksawakening.app.*;
import linksawakening.entity.*;
import linksawakening.world.room.*;
import linksawakening.world.transition.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
/** Synthetic scroll probe. Run from the repository root with the test runtime
 * classpath and user ROM available; outputs local screenshots under /tmp.
 * Room/Link state is injected after the real opening house input route.
 */
public class EntryProbe {
 static void png(byte[] rgba, Path out)throws Exception {
  BufferedImage img=new BufferedImage(160,144,BufferedImage.TYPE_INT_ARGB);
  for(int y=0;y<144;y++)for(int x=0;x<160;x++){int i=(y*160+x)*4;img.setRGB(x,y,0xff000000|((rgba[i]&255)<<16)|((rgba[i+1]&255)<<8)|(rgba[i+2]&255));}
  ImageIO.write(img,"png",out.toFile());
 }
 public static void main(String[] args)throws Exception {
  for(String b:new String[]{"right","left","up","down"}) {
   GameSession s=new GameSession();s.initializeHeadlessNewGame();
   for(String line:Files.readAllLines(Path.of("tools/opening/house-route.tsv"))){if(line.isBlank()||line.startsWith("#"))continue;String[] f=line.split(" ");int held=Integer.parseInt(f[1]),idle=Integer.parseInt(f[2]);if(held>0)s.headlessButton(f[0],true);for(int t=0;t<held+idle;t++){if(t==held&&held>0)s.headlessButton(f[0],false);s.headlessFrame();}}
   var d=s.diagnostics();var l=d.link();int x=b.equals("right")?148:b.equals("left")?-5:72;int y=b.equals("up")?-5:b.equals("down")?116:80;
   l.setPixelPosition(x,y);
   var mf=Link.class.getDeclaredField("linkMotion");mf.setAccessible(true);var m=mf.get(l);
   for(String n:new String[]{"subX","subY"}){var f=m.getClass().getDeclaredField(n);f.setAccessible(true);f.setInt(m,f.getInt(m)+(n.equals("subX")?10:5));}
   s.headlessButton(b,true);
   var c=new RoomTransitionCoordinator(d.roomSession(),new RoomBoundaryController(),d.transitionController(),d.scrollController());c.handleOverworldBoundary(l);
   System.out.println(b+" entry "+l.romEntityX()+","+l.romEntityY()+" room="+d.roomSession().currentRoomId());
   byte[] rgba=null;int count=0;while(d.scrollController().isActive()&&count++<100)rgba=s.headlessFrame();
   for(String n:new String[]{"subX","subY"}) {
    var f=m.getClass().getDeclaredField(n); f.setAccessible(true);
    System.out.println(b+" "+n+" fraction="+(f.getInt(m)&15));
   }
   System.out.println(b+" complete "+l.romEntityX()+","+l.romEntityY()+" speed="+l.romSpeedX()+","+l.romSpeedY()+" frames="+count);
   png(rgba,Path.of("/tmp/ladx-entry-controlled/java-"+args[0]+"-"+b+"-complete.png"));
  }
 }
}
