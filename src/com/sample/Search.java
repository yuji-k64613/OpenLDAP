package com.sample;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.*;

// http://d.hatena.ne.jp/end0tknr/20150319/1426716347
// http://blog.livedoor.jp/k_shin_pro/archives/43631076.html
// https://docs.oracle.com/javase/jp/6/api/javax/naming/ldap/SortControl.html
// https://docs.oracle.com/javase/jp/6/api/javax/naming/ldap/package-summary.html

/**
 * Created by konishiyuji on 2016/07/01.
 */
public class Search {
    //    private DirContext ctx;
    private LdapContext ctx;

    public static void main(String[] args) {
        Search lt = new Search();
        boolean result = lt.connect("ldap://192.168.33.100", "389",
                "dc=test,dc=local", "cn=Manager", "password");
        if (!result) {
            System.out.println("接続失敗");
            return;
        }

        String[] attrs = new String[]{"cn"};

        Object[] param = new String[2];
        param[0] = "Foo";
        param[1] = "Taro";

        lt.search("ou=People,dc=test,dc=local", attrs,
                "(|(cn={0})(cn={1}))", param, "cn"); // OR検索

        lt.close();
    }


    public boolean connect(
            String url,
            String port,
            String dn,
            String id,
            String password) {

        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            StringBuilder sb = new StringBuilder();
            sb.append(url);
            if (port != null) {
                sb.append(":").append(port);
            }
            env.put(Context.PROVIDER_URL, sb.toString());
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, id + "," + dn);
            env.put(Context.SECURITY_CREDENTIALS, password);

            //接続タイムアウトの設定(３秒)
            env.put("com.sun.jndi.ldap.connect.timeout", "3000");
            // コネクションプールを有効化
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            //アイドル接続のコネクションを自動的に削除する設定5分
            env.put("com.sun.jndi.connect.pool.timeout", "300000");
            //デバッグを全て
            env.put("com.sun.jndi.connect.pool.debug", "all");

//            ctx = new InitialDirContext(env);
            ctx = new InitialLdapContext(env, null);
        } catch (Exception e) {
            e.printStackTrace();
            if (ctx == null) return false;

            try {
                ctx.close();
            } catch (Exception ex) {
                // 無視
            }
            ctx = null;
            return false;
        }
        return true;
    }

    public void close() {
        if (ctx == null) return;

        try {
            ctx.close();
        } catch (Exception ex) {
            // 無視
        }
    }

    public void search(String baseDn, String[] attrs, String filter, Object[] args, String sortKey) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attrs);

//        SortControl sortControls = null;
//        try {
//            sortControls = new SortControl(sortKey, Control.CRITICAL);
//            ctx.setRequestControls(new Control[]{sortControls});
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (NamingException e) {
//            e.printStackTrace();
//        }

        try {
            int pageSize = 1;
            byte[] cookie = null;
            ctx.setRequestControls(new Control[]{
                    new PagedResultsControl(pageSize, Control.CRITICAL)});
            do {
                NamingEnumeration<SearchResult> result =
                        ctx.search(baseDn, filter, args, searchControls);

                // エントリを1件ずつ処理
                while (result.hasMore()) { //エントリ毎に処理
                    System.out.println("----------------------------------");
                    SearchResult sr = result.next(); //エントリ取得
                    NamingEnumeration attributes = sr.getAttributes().getAll();

                    while (attributes.hasMore()) { //エントリの各属性を処理
                        Attribute attr = (Attribute) attributes.nextElement();
                        Enumeration values = attr.getAll();
                        while (values.hasMoreElements()) {
                            System.out.println(attr.getID() + "="
                                    + values.nextElement());
                        }
                    }
                }

                cookie = null;
                Control[] controls = ctx.getResponseControls();
                if (controls != null) {
                    for (int i = 0; i < controls.length; i++) {
                        if (controls[i] instanceof PagedResultsResponseControl) {
                            PagedResultsResponseControl prrc =
                                    (PagedResultsResponseControl) controls[i];
                            cookie = prrc.getCookie();
                        } else {
                            // Handle other response controls (if any)
                        }
                    }
                }
                ctx.setRequestControls(new Control[]{
                        new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
            }
            while (cookie != null);
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
