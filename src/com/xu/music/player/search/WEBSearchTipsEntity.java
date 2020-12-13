package com.xu.music.player.search;

public class WEBSearchTipsEntity {

    private String hintInfo;
    private String matchCount;
    private String hot;

    public String getHintInfo() {
        return hintInfo;
    }

    public void setHintInfo(String hintInfo) {
        this.hintInfo = hintInfo;
    }

    public String getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(String matchCount) {
        this.matchCount = matchCount;
    }

    public String getHot() {
        return hot;
    }

    public void setHot(String hot) {
        this.hot = hot;
    }

    @Override
    public String toString() {
        return "WEBSearchTipsEntity [hintInfo=" + hintInfo + ", matchCount=" + matchCount + ", hot=" + hot + "]";
    }

}
