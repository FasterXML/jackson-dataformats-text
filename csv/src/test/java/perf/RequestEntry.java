package perf;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestEntry
{
    @JsonProperty("APP_ID") public long appId;
    @JsonProperty("USER_SCREEN_NAME") public String userScreenName;
    @JsonProperty("REPORTER_SCREEN_NAME") public String reportScreenName;
    @JsonProperty("EVENT_DATE") public String eventDate;
    @JsonProperty("HOST") public String host;
    @JsonProperty("PATH") public String path;
    @JsonProperty("USER_AGENT") public String userAgent;
    @JsonProperty("IP") public String ip;
    @JsonProperty("COOKIE") public String cookie;
    @JsonProperty("SUBDOMAIN") public String subdomain;
    @JsonProperty("REQUEST_METHOD") public String requestMethod; // or Enum
    @JsonProperty("TRACE") public String trace;
    @JsonProperty("REFERRER") public String referrer;
    @JsonProperty("RELOAD_COUNT") public int reloadCount;
    @JsonProperty("SESSION_ID") public String sessionId;
    @JsonProperty("ACTION") public String action;
    @JsonProperty("CONTENT") public String content;
    @JsonProperty("KILL_COUNT") public int killCount;
    @JsonProperty("ABUSE_TYPE") public String abuseType;
}
