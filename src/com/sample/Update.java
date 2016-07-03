package com.sample;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by konishiyuji on 2016/07/01.
 */
public class Update {
    private DirContext ctx;

    public static void main(String[] args) {
        Update lt = new Update();
        boolean result = lt.connect("ldap://192.168.33.100", "389",
                "dc=test,dc=local", "cn=Manager", "password");
        if (!result) {
            System.out.println("接続失敗");
            return;
        }

        //lt.update("cn=Foo,ou=People,dc=test,dc=local", "Updated");
        lt.add("cn=user,ou=People,dc=test,dc=local", "user", "name");
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

            ctx = new InitialDirContext(env);
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

    public void update(String baseDn, String str) {
        ModificationItem[] mods = new ModificationItem[1];

        Attribute attrSn = new BasicAttribute("sn", str);
        // 追加、削除、置換を指定する
        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attrSn);

        try {
            ctx.modifyAttributes(baseDn, mods);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void add(String baseDn, String name, String user) {
        Attribute objClasses = new BasicAttribute("objectclass");
        objClasses.add("person");

        Attribute cn = new BasicAttribute("cn", user);
        Attribute sn = new BasicAttribute("sn", name);
        Attributes orig = new BasicAttributes();
        orig.put(objClasses);
        orig.put(cn);
        orig.put(sn);

        try {
            ctx.createSubcontext(baseDn, orig);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}
