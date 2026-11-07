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
   if(args.length > 1) {
    // Match global motion phase independently of transition preparation time.
    var frame=GameSession.class.getDeclaredField("frameCounter"); frame.setAccessible(true);
    frame.setInt(s,(Integer.parseInt(args[1])-(b.equals("left")||b.equals("right")?40:32))&255);
   }
   byte[] rgba=null;int count=0;while(d.scrollController().isActive()&&count++<100)rgba=s.headlessFrame();
   for(String n:new String[]{"subX","subY"}) {
    var f=m.getClass().getDeclaredField(n); f.setAccessible(true);
    System.out.println(b+" "+n+" fraction="+(f.getInt(m)&15));
   }
   System.out.println(b+" complete "+l.romEntityX()+","+l.romEntityY()+" speed="+l.romSpeedX()+","+l.romSpeedY()+" frames="+count+" worldFrame="+s.diagnostics().frameCounter()+" collision="+l.romCollisionType());
   if(args.length > 2 && b.equals("right")) {
    byte[] solo=new byte[160*144*4], background=new byte[160*144*4];
    linksawakening.render.FrameScene.of(java.util.List.of(new linksawakening.render.LinkRenderLayer(l,d.scrollController()))).drawTo(solo,d.gpu());
    linksawakening.render.FrameScene.of(java.util.List.of(new linksawakening.render.RoomRenderLayer(d.roomSession().renderSnapshot(),d.scrollController(),d.transitionController()))).drawTo(background,d.gpu());
    int changed=0;
    for(int yy=l.pixelY();yy<l.pixelY()+16;yy++) for(int xx=l.pixelX();xx<l.pixelX()+16;xx++) {
     int ii=(yy*160+xx)*4;
     if(rgba[ii]!=background[ii]||rgba[ii+1]!=background[ii+1]||rgba[ii+2]!=background[ii+2]) changed++;
    }
    System.out.println("render animation="+l.romAnimationState()+" z="+l.romEntityZ()+" bed="+l.isMarinWakeUpBedVisible()+" visible pixels over background="+changed);
    png(solo,Path.of("/tmp/ladx-entry-controlled/link-only-right.png"));
    png(background,Path.of("/tmp/ladx-entry-controlled/background-only-right.png"));
   }
   png(rgba,Path.of("/tmp/ladx-entry-controlled/java-"+args[0]+"-"+b+"-complete.png"));
   if(args.length > 1 && b.equals("right")) for(int post=0;post<4;post++) {
    byte[] postRgba=s.headlessFrame();
    if(args.length > 2) png(postRgba,Path.of("/tmp/ladx-entry-controlled/java-right-post"+(post+1)+".png"));
    System.out.println("right post worldFrame="+s.diagnostics().frameCounter()+" x="+l.romEntityX()
        +" y="+l.romEntityY()+" speed="+l.romSpeedX()+","+l.romSpeedY()+" collision="+l.romCollisionType());
   }
  }
 }
}
