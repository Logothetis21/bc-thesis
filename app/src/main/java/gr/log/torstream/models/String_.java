package gr.log.torstream.models;

import java.io.Serializable;

public class String_ implements Serializable {
    public String data;
    public boolean hasDrawable = false;

    public String_(String data, boolean hasDrawable){
        this.data = data;
        this.hasDrawable = hasDrawable;
    }
}
