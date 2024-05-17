package org.vaadin.vcamera;

import com.vaadin.flow.component.ComponentEvent;

/**
 * @author: AnhTT47
 * @createdDate: 16/05/2024
 */
public class PictureTakenEvent extends ComponentEvent<VCamera> {
    private final String imageUrl;
    private final String base64;

    public PictureTakenEvent(VCamera source, boolean fromClient, String imageUrl, String base64) {
        super(source, fromClient);
        this.imageUrl = imageUrl;
        this.base64 = base64;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public String getBase64() {
        return base64;
    }
}
