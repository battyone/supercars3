package supercars3.sys;


import java.util.*;
import java.io.*;

public class ParameterParser {
  public class ParsingException extends IOException
  {	  
    public ParsingException(String msg)
    {
      super(msg);
    }
  }

  private static final String FIN_ILLEGALE_FICHIER = "Unexpected end of file";
  private static final String  INSERER_SEPARATEUR_ENTRE_C_ET_SUITE = "Insert separator between xxx";
  private static final String  SCALAIRE_ATTENDU = "Expected scalar ";
  private static final String  CARACTERE_ATTENDU = "Expected char ";
  private static final String  MOT_ATTENDU = "Expected word ";


    private static final char DELIMITEUR_DEBUT_BLOC = '<';
    private static final char DELIMITEUR_FIN_BLOC = '>';


    private static final char COMMENT_START = ';';
    private static final char UNDEFINED_CODE = '?';
    public static final String UNDEFINED_STRING = "?";

    private static final String END_OF_LINE = System.getProperty("line.separator");
    private static final String FIELD_SEPARATOR = " ";

    private static final int UNDEFINED_INTEGER = Integer.MAX_VALUE;
    private static final long UNDEFINED_LONG = Long.MAX_VALUE;
    private static final float UNDEFINED_FLOAT = Float.MAX_VALUE;

    private BufferedReader m_entree = null;
    private BufferedWriter m_sortie = null;
    private String m_nomFichier;
    private int m_indLigne=0;
 
    private boolean m_estDansCommentaire;
    private StringTokenizer m_jetons;
    private String m_motDejaLu = null;
    private boolean m_isOutput = false;
    private int m_current_indent = 0;

    /*---------------------*
     * Fonctions internes. *
     *---------------------*/

    private void error(String message) throws IOException
    {
      throw new ParsingException(m_nomFichier + " at line "+m_indLigne+": " + message);
    }

    private String readLine() throws IOException {

      String rval = m_entree.readLine().replace('\r', ' '); // fix CR chars
      m_indLigne++;
      return rval;
    }

    private String lisJetonSuivant() throws IOException {
        String mot = null;
        String texte;

        // Mot d�j� lu est un mot qu'on a remis dans la "pile"
        // il est � prendre en priorit�
        if (m_motDejaLu != null) {
            mot = m_motDejaLu;
            m_motDejaLu = null;
            return mot;
        }

        // Tant qu'il n'y a plus de jetons
        while (m_jetons == null || !m_jetons.hasMoreTokens() ||
               m_estDansCommentaire) {
            // lire une nouvelle ligne
            // si plus de ligne
            if ( (texte = readLine()) == null) {
                // sortir en erreur
                error(FIN_ILLEGALE_FICHIER);
            }
            else {
                // decouper la ligne
                m_jetons = new StringTokenizer(texte);

                // si ligne vide
                if (!m_jetons.hasMoreTokens()) {
                    continue;
                }

                mot = m_jetons.nextToken();
                m_estDansCommentaire =  (mot.charAt(0) == COMMENT_START);

                if (!m_estDansCommentaire)
                {
                    return mot;
                }
            }
        }
        return m_jetons.nextToken();
    }

    private String lisMotSuivant() throws IOException {
        String mot = lisJetonSuivant();

        // D�tecte les commentaires en milieu de ligne
        if (mot.charAt(0) == COMMENT_START) {
            m_estDansCommentaire = true;
            mot = lisJetonSuivant();
        }
        mot = mot.replaceAll("%20"," ");
        mot = mot.replaceAll("%LF","\n");

        return mot;
    }

 
    
    private void verifieDelimiteur(char delimiteurVoulu) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit et ve'rifie un caracte`re "de'limiteur".
     *
     *  INTERFACE
     *
     * \param  voulu : entre'e
     ** De'limiteur voulu.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     *
     * \param  Retourne : sortie
     ** true ou false.
     *
     *  DESCRIPTION
     *
     ** Sauter un e'ventuel se'parateur de te^te.
     ** Ve'rifier que l'on trouve alors le de'limiteur voulu, \
     ** puis ve'rifier que ce de'limiteur est bien suivi par un se'parateur ou par \
     ** FIN_FICHIER.
     ** En cas d'erreur, le`ve une ParsingException.
     *
     */
    {
        String mot;

        /* Begin */

        mot = lisMotSuivant();

        if (mot.charAt(0) == delimiteurVoulu) {
            if (!mot.equalsIgnoreCase(String.valueOf(delimiteurVoulu))) {
                error(INSERER_SEPARATEUR_ENTRE_C_ET_SUITE);
            }
        }
        else {
            error(CARACTERE_ATTENDU + delimiteurVoulu);
        }
    }




    private String lisScalaire(String nom_scalaire) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit un parame`tre scalaire.
     *
     *  INTERFACE
     *
     * \param  valeur : sortie
     ** Chai^ne lue.
     **
     * \param  nom_scalaire : entre'e
     ** Nom du scalaire a` lire.
     ****
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     *
     *  DESCRIPTION
     *
     ** Lit le fichier et ve'rifie que l'on trouve bien :
     ** - un de'limiteur de de'but de bloc,
     ** - un mot qui doit e^tre nom_scalaire,
     ** - un mot qui lu pour valeur,
     ** - un de'limiteur de fin de bloc,
     **
     ** En cas d'erreur termine par ParsingException et fournit un message dans \
     ** err_message.
     *
     */
    {

        /* Begin */

        String mot;

        /* si nom_scalaire n'a pas deja ete lu (par lit_type_bloc par exemple). */
        if (!nom_scalaire.equalsIgnoreCase("sans")) {
            try {
                verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);
            }
            catch (Exception ex) {
                error(SCALAIRE_ATTENDU + nom_scalaire);
            }
            mot = lisMotSuivant();
            if (!mot.equalsIgnoreCase(nom_scalaire)) {
                /* Mauvais nom de bloc. */
                error(SCALAIRE_ATTENDU + nom_scalaire +
                                    "  read " + mot);
            }
        }

        mot = lisMotSuivant();
        verifieDelimiteur(DELIMITEUR_FIN_BLOC);
        return mot;
    }

    private boolean testeIndefini(String nom_scalaire,
                                  String scalaire,
                                  boolean indefini_valide) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Teste si necessaire si un scalaire est inde'fini.
     *
     *  INTERFACE
     *
     * \param  valeur : entree
     ** Chaine scalaire a` tester.
     **
     * \param  nom_scalaire : entre'e
     ** Nom du scalaire a` lire.
     **
     * \param  indefini_valide : entre'e
     ** Boole'en indiquant si le code indefini est valide.
     ** Utiliser les constantes INDEFINI_VALIDE et INDEFINI_INVALIDE.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     *
     *  DESCRIPTION
     *
     ** Si indefini_valide, la chaine lue peut e^tre indefinie, \
     ** c'est a` dire e'gale a` CODE_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message dans \
     ** err_message.
     *
     */
    {
      boolean rval = false;
        if (scalaire.equalsIgnoreCase(String.valueOf(UNDEFINED_CODE)))
        {
          if (!indefini_valide) {
            error(nom_scalaire + " cannot be undefined");
          }
          rval = true;
        }
        return rval;
    }

    private ParameterParser(String nomFichier, boolean isOutput) throws
        IOException {
        m_nomFichier = nomFichier;
        m_estDansCommentaire = false;
        m_isOutput = isOutput;

        if (!m_isOutput) {
            m_entree = new BufferedReader(new FileReader(m_nomFichier));
        }
        else {
            m_sortie = new BufferedWriter(new FileWriter(m_nomFichier));
        }
    }


    /*---------------------*
     * Fonctions visibles. *
     *---------------------*/


    static public ParameterParser open(String nomFichier) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Ouvre un fichier de parame`tres en lecture.
     *
     *  INTERFACE
     *
     * \param  nom_fichier : entre'e
     ** Nom du fichier a` ouvrir.
     **
     * \param  Retourne : sortie
     ** Descripteur du fichier de parame`tres ouvert.
     *
     *  DESCRIPTION
     *
     ** En cas d'erreur le`ve une ParsingException.
     *
     */
    {
        return new ParameterParser(nomFichier, false);

    }

    static public ParameterParser create(String nomFichier) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Ouvre un fichier de parame`tres en lecture.
     *
     *  INTERFACE
     *
     * \param  nom_fichier : entre'e
     ** Nom du fichier a` ouvrir.
     **
     * \param  Retourne : sortie
     ** Descripteur du fichier de parame`tres ouvert.
     *
     *  DESCRIPTION
     *
     ** En cas d'erreur le`ve une ParsingException.
     *
     */
    {
        return new ParameterParser(nomFichier, true);
    }

    public void close()
    /*!
     *
     *  ROLE
     *
     ** Ferme un fichier de parame`tres.
     *
     *  INTERFACE
     *
     * \param  fichier : entre'e / sortie
     ** Fichier de parame`tres a` fermer.
     *
     *  DESCRIPTION
     *
     ** Ferme le fichier.
     ** Libe`re le descripteur de fichier de parame`tres.
     ** Ne produit pas d'erreur.
     *
     */
    {
        try {
            if (m_entree != null) {
                m_entree.close();
            }
            if (m_sortie != null) {
                m_sortie.close();
            }
        }
        catch (Exception e) {
        }
        m_entree = null;
        m_sortie = null;
    }

    public void startBlockWrite(String nomBloc) throws IOException {
      indent();
      m_sortie.write(DELIMITEUR_DEBUT_BLOC + FIELD_SEPARATOR + nomBloc + END_OF_LINE);
      m_current_indent++;
    }

    public void endBlockWrite() throws IOException {
      m_current_indent--;
      indent();
      m_sortie.write(DELIMITEUR_FIN_BLOC + END_OF_LINE);
    }

    public void write(String ident, String[] values) throws IOException
    {
    	indent();
        m_sortie.write(DELIMITEUR_DEBUT_BLOC + " " + ident);
        for (String value : values)
        {
        	m_sortie.write(" " +value.replaceAll(" ","%20").replaceAll("\n","%LF"));
        }
        m_sortie.write(" "+DELIMITEUR_FIN_BLOC+END_OF_LINE);
    }
    public void write(String ident, int[] values) throws IOException
    {
    	indent();
        m_sortie.write(DELIMITEUR_DEBUT_BLOC + " " + ident);
        for (int value : values)
        {
        	m_sortie.write(" " +value);
        }
        m_sortie.write(" "+DELIMITEUR_FIN_BLOC+END_OF_LINE);
    }

    public void write(String ident, Object value) throws IOException 
    {
    	write(ident, value.toString());
    }
    public void write(String ident, int value) throws IOException {
        write(ident, Integer.toString(value));
    }

    public void write(String ident, long value) throws IOException {
        write(ident, Long.toString(value));
    }


    public void write(String ident, float value) throws IOException {
        write(ident, Float.toString(value));
    }
    public void write(String ident, double value) throws IOException {
           write(ident, Double.toString(value));
       }


         public void write(String ident, boolean value) throws IOException {
        write(ident, value ? "yes" : "no");
    }

    public void write(String ident, String value) throws
        IOException {
      indent();
        m_sortie.write(DELIMITEUR_DEBUT_BLOC + " " + ident + " " +
                       value.replaceAll(" ","%20").replaceAll("\n","%LF") + " " +
                       DELIMITEUR_FIN_BLOC+END_OF_LINE);
    }

    private void indent() throws IOException
    {
      for (int i = 0; i < m_current_indent;i++)
       {
         m_sortie.write(' ');
       }
    }
    public String readBlockName() throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit une structure de de'but de bloc.
     *
     *  INTERFACE
     *
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     **
     * \param  Retourne : sortie
     ** Nom du bloc lu.
     *
     *  DESCRIPTION
     *
     ** Lit le fichier et ve'rifie que l'on trouve bien un \
     ** de'limiteur de de'but de bloc suivi d'un mot qui est lu comme \
     ** le nom du bloc.
     ** En cas d'erreur termine par ParsingException et fournit un message \
     ** dans err_message.
     *
     */
    {
        String nom_bloc = null;

        /* Begin */

        verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);
        nom_bloc = lisMotSuivant();

        return nom_bloc;
    }








    public void  startBlockVerify(String nom_bloc) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit et ve'rifie une structure de de'but de bloc.
     *
     *  INTERFACE
     *
     * \param  nom_bloc : entre'e
     ** Nom du bloc a` ve'rifier.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     *
     *  DESCRIPTION
     *
     ** Lit le fichier et ve'rifie que l'on trouve bien un \
     ** de'limiteur de de'but de bloc suivi du mot nom_bloc.
     ** Si nom_bloc = "sans" alors rien n'est fait.
     ** En cas d'erreur termine par ParsingException et fournit un message dans \
     ** err_message.
     *
     */
    {
        String mot;

        /* Begin */

        if (!nom_bloc.equalsIgnoreCase("sans")) 
        {

        	verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);

        	mot = lisMotSuivant();

        	if (!nom_bloc.equalsIgnoreCase(mot)) 
        	{
        		/* Mauvais nom de bloc. */
        		error(MOT_ATTENDU +
        				nom_bloc + " instead of " + mot);
        	}


        }
    }


    public void endBlockVerify() throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit et ve'rifie une structure de fin de bloc.
     *
     *  INTERFACE
     *
     * \param  nom_bloc : entre'e
     ** Nom du bloc a` ve'rifier.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     *
     *  DESCRIPTION
     *
     ** Lit le fichier et ve'rifie que l'on trouve bien \
     ** un de'limiteur de fin de bloc.
     ** En cas d'erreur termine par ParsingException et fournit un message dans \
     ** err_message.
     *
     */
    {
        verifieDelimiteur(DELIMITEUR_FIN_BLOC);
    }


    public long readLong(String nom_scalaire) throws IOException
    {
      return readLong(nom_scalaire,false);
    }
        public long readLong(String nom_scalaire,
                              boolean indefini_valide) throws IOException

 
    {
        String scalaire = null;

        /* Begin */

        scalaire = lisScalaire(nom_scalaire);

        if (testeIndefini(nom_scalaire,
                          scalaire,
                          indefini_valide)) {
            return UNDEFINED_LONG;
        }
        
        return Long.parseLong(scalaire); 
            
    }

    public int readInteger(String nom_scalaire) throws IOException
    {
      return readInteger(nom_scalaire,false);
    }
        public int readInteger(String nom_scalaire,
                              boolean indefini_valide) throws IOException

   /*!
     *
     *  ROLE
     *
     ** Lit un scalaire de type entier.
     *
     *  INTERFACE
     *
     * \param  nom_scalaire : entre'e
     ** Nom du scalaire a` lire.
     **
     * \param  indefini_valide : entre'e
     ** Boole'en indiquant si le code indefini est valide.
     ** Utiliser les constantes INDEFINI_VALIDE et INDEFINI_INVALIDE.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     **
     * \param  Retourne : sortie
     ** Valeur du scalaire lu.
     *
     *  DESCRIPTION
     *
     ** Le nume'rique doit correspondre strictement au type entier \
     ** (ne doit pas e^tre suivi d'autres caracte`res).
     ** Si indefini_valide et si la chaine lue est indefinie, \
     ** alors renvoie ENTIER_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message \
     ** dans err_message.
     *
     */
    {
        String scalaire = null;

        /* Begin */

        scalaire = lisScalaire(nom_scalaire);

        if (testeIndefini(nom_scalaire,
                          scalaire,
                          indefini_valide)) {
            return UNDEFINED_INTEGER;
        }
        
        return Integer.parseInt(scalaire);
        
    }



    public float readFloat(String nom_scalaire,
                         boolean indefini_valide) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit un scalaire de type reel.
     *
     *  INTERFACE
     *
     * \param  nom_scalaire : entre'e
     ** Nom du scalaire a` lire.
     **
     * \param  indefini_valide : entre'e
     ** Boole'en indiquant si le code indefini est valide.
     ** Utiliser les constantes INDEFINI_VALIDE et INDEFINI_INVALIDE.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     **
     * \param  Retourne : sortie
     ** Valeur du scalaire lu.
     *
     *  DESCRIPTION
     *
     ** Le nume'rique doit correspondre strictement au type reel \
     ** (ne doit pas e^tre suivi d'autres caracte`res).
     ** Si indefini_valide et si la chaine lue est indefinie, \
     ** alors renvoie REEL_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message dans \
     ** err_message.
     *
     */
    {
        String scalaire = null;

        /* Begin */

        scalaire = lisScalaire(nom_scalaire);

        if (testeIndefini(nom_scalaire,
                          scalaire,
                          indefini_valide)) {
            return UNDEFINED_FLOAT;
        }
        
        return Float.parseFloat(scalaire);
        
    }

    /**
     * Lit un scalaire de type reel. Ne peut �tre ind�fini
     * @param nom_scalaire nom du scalaire � lire
     * @return le flottant lu
     * @throws IOException
     */
    public float readFloat(String nom_scalaire) throws IOException {
        return readFloat(nom_scalaire, false);
    }

    public String readString(String nom_scalaire,
                            boolean indefini_valide) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit un scalaire de type entier.
     *
     *  INTERFACE
     *
     * \param  nom_scalaire : entre'e
     ** Nom du scalaire a` lire.
     **
     * \param  indefini_valide : entre'e
     ** Boole'en indiquant si le code indefini est valide.
     ** Utiliser les constantes INDEFINI_VALIDE et INDEFINI_INVALIDE.
     **
     * \param  fichier : entre'e
     ** Fichier de parame`tres ou` lire.
     **
     * \param  Retourne : sortie
     ** Valeur du scalaire lu.
     *
     *  DESCRIPTION
     *
     ** Le nume'rique doit correspondre strictement au type entier \
     ** (ne doit pas e^tre suivi d'autres caracte`res).
     ** Si indefini_valide et si la chaine lue est indefinie, \
     ** alors renvoie ENTIER_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message \
     ** dans err_message.
     *
     */
    {
        String scalaire = null;

        /* Begin */

        scalaire = lisScalaire(nom_scalaire);

        testeIndefini(nom_scalaire,
                      scalaire,
                      indefini_valide);
        return scalaire;
    }

    /**
     * Lit un scalaire de type String. Ne peut �tre ind�fini
     * @param nom_scalaire nom du scalaire � lire
     * @return la chaine lue
     * @throws IOException
     */
    public String readString(String nom_scalaire) throws IOException {
        return readString(nom_scalaire, false);
    }

    public boolean readBoolean(String nom_scalaire) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Le nume'rique doit correspondre strictement au type entier \
     ** (ne doit pas e^tre suivi d'autres caracte`res).
     ** Si indefini_valide et si la chaine lue est indefinie, \
     ** alors renvoie ENTIER_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message \
     ** dans err_message.
     *
     */
    {
        String scalaire = null;
        boolean rval = false;
        /* Begin */

        scalaire = lisScalaire(nom_scalaire);

        if (scalaire.equalsIgnoreCase("yes")) {
            rval = true;
        }
        else if (scalaire.equalsIgnoreCase("no")) {
            rval = false;
        }
        else {
            error("after " + nom_scalaire +
                                " : boolean expected");
        }
        return rval;
    }

    public void readVector(String nom_vecteur,
                           String vecteur[]) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Lit un vecteur de String.
     *
     ** Le nume'rique doit correspondre strictement au type entier \
     ** (ne doit pas e^tre suivi d'autres caracte`res).
     ** Si indefini_valide et si la chaine lue est indefinie, \
     ** alors renvoie ENTIER_INDEFINI.
     ** En cas d'erreur termine par ParsingException et fournit un message \
     ** dans err_message.
     *
     */
    {
        String mot = null;
        int ind_elem;
        int nb_elements = vecteur.length;

        /* Begin */

        verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);
        mot = lisMotSuivant();
        if (!mot.equalsIgnoreCase(nom_vecteur)) {
            /* Mauvais nom de bloc. */
            error(SCALAIRE_ATTENDU +
                                nom_vecteur);
        }

        for (ind_elem = 0; ind_elem < nb_elements; ind_elem++) {
            vecteur[ind_elem] = lisMotSuivant();
            testeIndefini(nom_vecteur,
                          vecteur[ind_elem],
                          false);
        }

        verifieDelimiteur(DELIMITEUR_FIN_BLOC);

    }



    public void readVector(String nom_vecteur,
            float vecteur[]) throws IOException
/*!
*
*  ROLE
*
** Lit un vecteur de flottant.
*
*/
{
String mot = null;
int ind_elem;
int nb_elements = vecteur.length;
String reel;

/* Begin */
verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);
mot = lisMotSuivant();
if (!mot.equalsIgnoreCase(nom_vecteur)) {
/* Mauvais nom de bloc. */
error(SCALAIRE_ATTENDU +
            nom_vecteur);
}

for (ind_elem = 0; ind_elem < nb_elements; ind_elem++) {
reel = lisMotSuivant();
testeIndefini(nom_vecteur,reel,false);
vecteur[ind_elem] = Float.parseFloat(reel);
}

verifieDelimiteur(DELIMITEUR_FIN_BLOC);

	}
	    public void readVector(String nom_vecteur,
	            int vecteur[]) throws IOException
	            /*!
	             *
	             *  ROLE
	             *
	             ** Lit un vecteur de flottant.
	             *
	             */
	            {
	    	String mot = null;
	    	int ind_elem;
	    	int nb_elements = vecteur.length;
	    	String reel;

	    	/* Begin */
	    	verifieDelimiteur(DELIMITEUR_DEBUT_BLOC);
	    	mot = lisMotSuivant();
	    	if (!mot.equalsIgnoreCase(nom_vecteur)) {
	    		/* Mauvais nom de bloc. */
	    		error(SCALAIRE_ATTENDU +
	    				nom_vecteur);
	    	}

	    	for (ind_elem = 0; ind_elem < nb_elements; ind_elem++) {
	    		reel = lisMotSuivant();
	    		testeIndefini(nom_vecteur,reel,false);
	    		vecteur[ind_elem] = Integer.parseInt(reel);
	    	}

	    	verifieDelimiteur(DELIMITEUR_FIN_BLOC);

	            }

    
    public int readEnumerate(String nom_enum,
                       String e[]) throws IOException
    /*!
     *
     *  ROLE
     *
     ** Verifie que la valeur lue fait partie de l'enum�r�.
     *
     */
    {
        String mot = null;
        int ind_elem = 0;

        /* Begin */

        mot = readString(nom_enum);
        while (ind_elem < e.length && !mot.equalsIgnoreCase(e[ind_elem])) {
            ind_elem++;

        }
        if (ind_elem >= e.length) {

            error("word " + mot + " is not within list");
        }
        return ind_elem;

    }

    // Les fonctions set et get
    public String getFileName() {
        return m_nomFichier;
    }
}


