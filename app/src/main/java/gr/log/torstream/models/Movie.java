package gr.log.torstream.models;

import java.io.Serializable;
import java.util.ArrayList;

public class Movie implements Serializable {
    public String name;
    public String imbd_code;
    public String background_img;
    public String poster_img;
    public String logo_img;
    public String description;

    public ArrayList<String_> runtime_year_imdb;
    public ArrayList<String_> genres;
    public ArrayList<String_> director;
    public ArrayList<String_> cast;
    public ArrayList<String_> writer;
}
