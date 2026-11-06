export const msalConfig = {
    auth: {
        clientId: "7ec7e5ba-9693-4758-9e2f-73c0900cd68d",

        authority:
            "https://login.microsoftonline.com/a2d774d1-6d61-457f-8298-3217272b8234",

        // Usa el origen donde corre la app (localhost o el dominio del tunel)
        redirectUri: window.location.origin + "/"
    },

    cache: {
        cacheLocation: "sessionStorage"
    }
};

export const loginRequest = {
    scopes: ["openid", "profile", "email"]
};

export const apiRequest = {
    scopes: [
        "api://7ec7e5ba-9693-4758-9e2f-73c0900cd68d/access_as_user"
    ]
};