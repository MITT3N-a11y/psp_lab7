package by.charity.shared.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Action {
        LOGIN, LOGOUT, CHANGE_PASSWORD,
        GET_ALL_USERS, GET_USER_BY_ID, CREATE_USER,
        UPDATE_USER, DEACTIVATE_USER, DELETE_USER,
        UPDATE_PROFILE, GET_MY_PROFILE,
        GET_ALL_FUNDS, GET_FUND_BY_ID, CREATE_FUND,
        UPDATE_FUND, DEACTIVATE_FUND, DELETE_FUND,
        GET_ALL_PROJECTS, GET_PROJECTS_BY_FUND, GET_PROJECT_BY_ID,
        CREATE_PROJECT, UPDATE_PROJECT, UPDATE_PROJECT_STATUS, DELETE_PROJECT,
        GET_ALL_DONATIONS, GET_DONATIONS_BY_FUND, GET_DONATIONS_BY_PROJECT,
        REGISTER_DONATION, UPDATE_DONATION, DELETE_DONATION,
        GET_TOP_DONORS,
        GET_ALL_REPORTS, GET_PUBLIC_REPORTS, GET_REPORT_BY_ID,
        GENERATE_REPORT, DELETE_REPORT,
        GET_FUND_STATISTICS, GET_DONATION_STATISTICS,
        GET_DASHBOARD_DATA,
        REGISTER_GUEST,
        ADD_FUND_EXPENSE
    }

    private Action action;
    private String sessionToken;
    private Map<String, Object> params;

    public Request() {
        this.params = new HashMap<>();
    }

    public Request(Action action, String sessionToken) {
        this();
        this.action = action;
        this.sessionToken = sessionToken;
    }

    public void addParam(String key, Object value) {
        this.params.put(key, value);
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String token) { this.sessionToken = token; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}