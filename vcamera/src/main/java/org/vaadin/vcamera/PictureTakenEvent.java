package org.vaadin.vcamera;

import com.vaadin.flow.component.ComponentEvent;

/**
 * @author: AnhTT47
 * @createdDate: 16/05/2024
 */
public class PictureTakenEvent extends ComponentEvent<VCamera> {
    private final String imageUrl;

    public PictureTakenEvent(VCamera source, boolean fromClient, String imageUrl) {
        super(source, fromClient);
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
