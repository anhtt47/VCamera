package org.vaadin.vcamera;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.shared.Registration;

/**
 * A special video element that streams content from browser camera.
 * <p>During streaming, users can record still or video clips of the stream, that browser send to the server. The data can be accessed using the DataReceiver interface, see {@link #setReceiver(DataReceiver)}.</p>
 */
@Tag("video")
public class VCamera extends Component {

    private boolean cameraOn;
    private boolean recording;

    public VCamera() {
        getElement().setProperty("volume", 0);
        getElement().addEventListener("picture-taken", e -> {
            /*System.out.println("Event 'picture-taken' triggered");*/
            String imageUrl = e.getEventData().getString("event.detail");
            fireEvent(new PictureTakenEvent(this, true, imageUrl));
        }).addEventData("event.detail");
    }

    public void setReceiver(DataReceiver receiver) {
        getElement().setAttribute("target", new StreamReceiver(
                getElement().getNode(), "camera", new CameraStreamVariable(receiver)));
    }

    private void fireFinishedEvent(String mime) {
        fireEvent(new FinishedEvent(this, true, mime));
    }

    public void startRecording() {
        if(!cameraOn) {
            throw new IllegalStateException(ErrorDescription.ERR_CAMERA_OFF);
        }
        recording = true;
        getElement().executeJs("""
                let target = this.getAttribute("target");;
                this.recorder = new MediaRecorder(this.stream);
                this.recorder.ondataavailable = e => {
                    let formData = new FormData();
                    formData.append("data", e.data);
                    fetch(target, {
                        method: "post",
                        body: formData
                    }).then(response => console.log(response));
                }
                this.recorder.start();
                    """);
    }

    public void stopRecording() {
        if(!recording) {
            throw new IllegalStateException("Not recording");
        }
        getElement().executeJs("this.recorder.stop()");
        recording = false;
    }

    public void closeCamera() {
        cameraOn = false;
        getElement().executeJs("""
                if(this.stream!=null) {
                    this.stream.getTracks().forEach( t=> {
                        t.stop();
                    });
                    this.stream = null;
                }
                """);
    }

    public void takePicture() {
        if(!cameraOn) {
            throw new IllegalStateException(ErrorDescription.ERR_CAMERA_OFF);
        }
        getElement().executeJs("""
                let canvas = document.createElement("canvas");
                let context = canvas.getContext('2d');
                let target = this.getAttribute("target");;
                canvas.height = this.videoHeight;
                canvas.width = this.videoWidth;
                context.drawImage(this, 0, 0, this.videoWidth, this.videoHeight);
                canvas.toBlob(b => {
                    let formData = new FormData();
                    formData.append("data",b);
                    fetch(target, {
                        method: "post",
                        body: formData
                    }).then(response => console.log(response));
                },'image/jpeg',0.95);
                """);
    }
    /** @author: AnhTT47
     *  @purpose: Capture image and return url on local to show
    * */
    public void takePictureLocal() {
        if (!cameraOn) {
            throw new IllegalStateException(ErrorDescription.ERR_CAMERA_OFF);
        }
        getElement().executeJs("""
            let canvas = document.createElement("canvas");
            let context = canvas.getContext('2d');
            canvas.height = this.videoHeight;
            canvas.width = this.videoWidth;
            context.drawImage(this, 0, 0, this.videoWidth, this.videoHeight);
            canvas.toBlob(b => {
                let url = URL.createObjectURL(b);
                this.dispatchEvent(new CustomEvent('picture-taken', { detail: url }));
                console.log("Image: "+ url)
            }, 'image/jpeg', 1.0);
            """);
    }


    public void openCamera() {
        openCamera("{audio:true,video:true}");
    }

    public void openCamera(String optionsJson) {
        cameraOn = true;
        getElement().executeJs("""
                if(this.stream == null) {
                    if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                        navigator.mediaDevices.getUserMedia(%s).then(stream => {
                            this.stream = stream;
                            this.srcObject = this.stream;
                            this.play();
                        });
                    }
                }
                        """.formatted(optionsJson));
    }

    public boolean isCameraOpen() {
        return cameraOn;
    }

    public Registration addFinishedListener(ComponentEventListener<FinishedEvent> listener) {
        return addListener(FinishedEvent.class, listener);
    }

    public Registration addPictureTakenListener(ComponentEventListener<PictureTakenEvent> listener) {
        return addListener(PictureTakenEvent.class, listener);
    }


    private class CameraStreamVariable implements StreamVariable {

        String mime;
        DataReceiver receiver;

        public CameraStreamVariable(DataReceiver receiver) {
            this.receiver = receiver;
        }


        @Override
        public OutputStream getOutputStream() {
            return receiver.getOutputStream(mime);
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        @Override
        public boolean listenProgress() {
            return false;
        }

        @Override
        public void onProgress(StreamingProgressEvent arg0) {

        }

        @Override
        public void streamingFailed(StreamingErrorEvent arg0) {

        }

        @Override
        public void streamingFinished(StreamingEndEvent arg0) {
            fireFinishedEvent(mime);

        }

        @Override
        public void streamingStarted(StreamingStartEvent arg0) {
            mime = arg0.getMimeType();
        }

    }

}
