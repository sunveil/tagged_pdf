package model;

import java.util.ArrayList;
import java.util.List;

public class Page {

    private List<Tag> tags;

    public Page(){
        tags = new ArrayList<>();
    }

    public void addTag(Tag tag){
        tags.add(tag);
    }

    public List<Tag> getTags(){
        return tags;
    }

}
