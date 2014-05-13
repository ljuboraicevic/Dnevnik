package rs.edu.megatrend.dnevnik;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author ljubo
 */
public class ConnectionBase {
    
    private static final String protocol = "sqlite";
    private static final String filename = "ednevnik.sqlite";
    
    //public static String hostName = "localhost";
    //public static String port     = "5432";
    //public static String dbName   = "postgres";
    //public static String username = "postgres";
    //public static String password = "postgres";
    
    private static String getParams4Conn()
    {
        //za mysql     jdbc:mysql://localhost/dzkula_proba?useUnicode=true&characterEncoding=utf8&user=root&password=enter
        //za postgres return "jdbc:"+protocol+"://"+hostName+":"+port+"/"+dbName+"?user="+username+"&password="+password;
        return "jdbc:"+protocol+":"+filename;
    }
            
    public static Connection conn; 
    
    public ConnectionBase() {}
    
    public static Connection getConnection() throws ClassNotFoundException, SQLException
    {
        Class.forName("org.sqlite.JDBC");
        try {
                conn = DriverManager.getConnection(getParams4Conn());
        } catch (SQLException ex) {
            Logger.getLogger(ConnectionBase.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "<html>Izgubljena je konekcija sa serverom!<br>Program će se sada ugasiti. Postarajte se da je internet konekcija u redu, a zatim ponovo pokrenite program.</html>");
            System.exit(1);
        }
        return conn;
    }

    //izvrsiQuery prima query, a vraca resultset i dobija konekciju koristeci klasu ConnectionBase
    public static ResultSet izvrsiQuery(String kveri)
    {
        //ako prosledjeni kveri nije prazan string
        if (kveri.length() != 0)
        {
            //dobija konekciju od classe ConnectionBase (ona vraca uvek jednu konekciju, da se ne bi pravilo vise konekcija)
            try
            {
                Connection konekcija = ConnectionBase.getConnection();
                Statement st = konekcija.createStatement();
                ResultSet rs = st.executeQuery(kveri);
                return rs;
            }
            catch (ClassNotFoundException cnfe) {
                 Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, cnfe);
                return null;
            } catch (SQLException sqle) {
                Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, sqle);      
                return null;
            }
            finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionBase.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        //ako jeste vrati null
        else
        {
            return null;
        }
    }
    
    public static void izvrsiQueryBezRezultata(String kveri)
    {
        //ako prosledjeni kveri nije prazan string
        //ako jeste, nista se nece desiti
        if (kveri.length() != 0)
        {
            try
            {
                Connection konekcija = ConnectionBase.getConnection();
                Statement st = konekcija.createStatement();
                st.execute(kveri);
            }
            catch (ClassNotFoundException cnfe) {
                 Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, cnfe);      
            } catch (SQLException sqle) {
                Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, sqle);      
            }
            finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectionBase.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public static DefaultTableModel napraviROTableModel(ResultSet rs)
    {
                
        //overriduje se isCellEditable da uvek vraca false, da bi model,
        //a s njim i tabela bili read-only
        DefaultTableModel aModel = new DefaultTableModel()
        {
            @Override
            public boolean isCellEditable(int row, int column) {
            return false;
        }
        };
                
        try
        {
            //iz metadata restultseta dobija se broj kolona
            int brojKolona = rs.getMetaData().getColumnCount();
            
            //pravi se niz stringova koji ima clanova koliko postoiji kolona u tabeli
            String[] naziviKolona = new String[brojKolona];
            
            //kroz petlju se dodaju imena kolona u niz
            for (int iCount = 0; iCount < brojKolona; iCount++)
            {
                naziviKolona[iCount] = rs.getMetaData().getColumnLabel(iCount+1);
            }
            
            //niz stringova naziviKolona se postavlja modelu, da ga koristi kao nazive kolona
            aModel.setColumnIdentifiers(naziviKolona);
        
            //model se popunjava redovima iz resultset-a
            //gde god se koristi resultset mora se staviti try catch za SQLException
            while (rs.next())
            {
                //pravi se niz objekata koji ima onoliko clanova koliko ima kolona u tabeli
                Object[] objekti = new Object[brojKolona];
                
                //kroz petlju se svakom clanu tog niza dodeljuje odgovarajuca vrednost iz resultseta
                for (int iCount = 0; iCount < brojKolona; iCount++)
                {
                    objekti[iCount] = rs.getObject(iCount+1);
                }
                
                //taj niz se dodaje modelu kao novi red
                aModel.addRow(objekti);
            }
        }
        catch (SQLException sqle) {
            Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, sqle);
        }
        
        //funkcija vraca napravljeni model
        return aModel;
    }
    
    //ComboHolder je klasa od koje ce se praviti vektor, koji ce se koristiti za popunjavanje ComboBoxa
    class ComboHolder 
    {
        final private String display;
        final private int num;

        public ComboHolder(int num, String display)
        {
            this.display = display;
            this.num = num;
        }

        @Override
        public String toString()
        {
            return display;
        }

        public int getNum()
        {
            return num;
        }
    }
    
    //da bi cbmodel bio napravljen metodom napraviComboBoxModel ResultSet koji mu se prosledjuje mora da vraca
    //samo dve kolone - prva je primary key - druga je vrednost koja ce biti ispisana u comboboxu
    public DefaultComboBoxModel napraviComboBoxModel(ResultSet rs)
    {
        //classa ComboHolder je nested classa definisana gore ^
        //cbModel je vector te klase
        Vector<ComboHolder> cbModel = new Vector<ComboHolder>();
        
        try
        {
            //dobija meta podatke o prosledjenom resultsetu
            ResultSetMetaData meta = rs.getMetaData();
            
            //u slucaju da je broj kolona dva
            if (meta.getColumnCount() == 2)
            {
                //nalazi imena kolona
                String firstColumnName = meta.getColumnName(1);
                String secondColumnName = meta.getColumnName(2);
                
                //i kroz petlju dodaje odgovarajuca polja u vektor
                while (rs.next())
                {
                    cbModel.add(new ComboHolder(rs.getInt(firstColumnName), rs.getString(secondColumnName)));
                }
            }
            //ako je broj kolona razlicit od dva
            else
            {
                //vraca gresku koja ce biti ispisana u comboboxu
                cbModel.add(new ComboHolder(0,"Wrong number of columns in resultset"));
            }
        }
        catch (SQLException sqle) {
            Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, sqle);      
        }
    
        return new DefaultComboBoxModel(cbModel);
    }
    
    public static int vratiPozicijuNaOsnovuPrimaryKeyUCBModelu(javax.swing.ComboBoxModel cbModel, String vrednost)
    {
        //petljom se prolazi kroz vektor cbModel, dok se ne nadje na kojoj poziciji
        //se nalazi prosledjeno odeljenje, i onda se combobox cbOdeljenje postavi na tu poziciju
        for (int iCount = 0; iCount < cbModel.getSize(); iCount++)
        {
            if (String.valueOf(((ComboHolder)cbModel.getElementAt(iCount)).getNum()).equals(vrednost))
            {
                return iCount;
            }
        }
        return 0;
    }
    
    public static String vratiPrimaryKeyNaOsnovuPozicijeUCBModelu(javax.swing.ComboBoxModel cbModel, int pozicija)
    {
       return String.valueOf(((ComboHolder)cbModel.getElementAt(pozicija)).getNum());
    }   
    
    //pass se u tabeli cuva kao md5 hash pravog passworda; ova metoda
    //pretvara string unet u polju u md5 koji je uporediv s podacima iz tabele
    public static String string2md5(String s)
    {
        String hashTekst = "";
        try {
            byte[] bajtovaUSifri = s.getBytes("UTF-8");
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(bajtovaUSifri);
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            hashTekst = bigInt.toString(16);
            //u slucaju da md5 bude kraci od 32, treba ta mesta ispred popuniti nulama
            while (hashTekst.length() < 32)
            {
                hashTekst = "0" + hashTekst;
            }
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
                Logger.getLogger(fLog.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return hashTekst;
    }
}
