package net.orbyfied.hscsms.db;

public class Login {

    public static URILogin ofURI(String uri, String db) {
        return new URILogin(uri, db);
    }

    //////////////////////

    public static class URILogin extends Login {

        protected final String uri;
        protected final String db;

        public URILogin(String uri, String db) {
            this.uri = uri;
            this.db  = db;
        }

        public String getURI() {
            return uri;
        }

        public String getDatabase() {
            return db;
        }

    }

}
