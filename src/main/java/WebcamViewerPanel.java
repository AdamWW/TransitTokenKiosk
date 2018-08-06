import com.github.sarxos.webcam.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class WebcamViewerPanel extends JPanel implements Runnable, WebcamListener, Thread.UncaughtExceptionHandler, ItemListener, WebcamDiscoveryListener {

    private Webcam webcam = null;
    private WebcamPanel panel = null;
    private final ImageFoundListener imageFoundListener;

    public WebcamViewerPanel(ImageFoundListener imageFoundListener) {
        this.imageFoundListener = imageFoundListener;
    }

    public void resume() {
        panel.resume();
    }

    public void pause() {
        panel.pause();
    }

    public void close() {
        webcam.close();
    }

    //Runnable methods:
    @Override
    public void run() {
        Webcam.addDiscoveryListener(this);

        setLayout(new BorderLayout());


        webcam = Webcam.getDefault();

        if (webcam == null) {
            System.out.println("No webcams found...");
            System.exit(1);
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.addWebcamListener(this);

        panel = new WebcamPanel(webcam, false);
        panel.setFPSDisplayed(false);

//        add(picker, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        Thread t = new Thread(() -> panel.start());
        t.setName("webcam-viewer-starter");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(this);
        t.start();
    }

    //ItemListener methods:
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getItem() != webcam) {
            if (webcam != null) {

                panel.stop();

                remove(panel);

                webcam.removeWebcamListener(this);
                webcam.close();

                webcam = (Webcam) e.getItem();
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.addWebcamListener(this);

                panel = new WebcamPanel(webcam, false);
                panel.setFPSDisplayed(true);

                add(panel, BorderLayout.CENTER);

                Thread t = new Thread(() -> panel.start());
                t.setName("webcam-viewer-stopper");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(this);
                t.start();
            }
        }
    }


    //WebcamListener methods:
    @Override
    public void webcamOpen(WebcamEvent we) {
        System.out.println("webcam open");
    }

    @Override
    public void webcamClosed(WebcamEvent we) {
        System.out.println("webcam closed");
    }

    @Override
    public void webcamDisposed(WebcamEvent we) {
        System.out.println("webcam disposed");
    }

    @Override
    public void webcamImageObtained(WebcamEvent we) {
        Image image = we.getImage();
        imageFoundListener.imageFound(image);
    }

    //WebcamDiscoveryListener methods:
    @Override
    public void webcamFound(WebcamDiscoveryEvent event) {
//        if (picker != null) {
//            picker.addItem(event.getWebcam());
//        }
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent event) {
//        if (picker != null) {
//            picker.removeItem(event.getWebcam());
//        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println(String.format("Exception in thread %s", t.getName()));
        e.printStackTrace();
    }

    public interface ImageFoundListener {
        void imageFound(Image image);
    }
}
