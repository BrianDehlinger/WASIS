package br.unicamp.fnjv.wasis.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.leandrotacioli.libs.LTDataTypes;

/**
 * Cria um gerenciador de conexão de bancos de dados.
 * Baseando-se no arquivo de parâmetros, o sistema
 * verifica qual banco de dados está sendo utilizado e
 * fornece o acesso. <br>
 * Somente é possível conexões com bancos de dados MySQL e H2.
 * 
 * @author Leandro Tacioli
 * @version 1.0 - 15/Set/2014
 */
public class DatabaseConnection {
	private static DatabaseConnection objDatabaseConnection;
	
	/**
	 * Cria um gerenciador de conexão de bancos de dados.
	 * Baseando-se no arquivo de parâmetros, o sistema
	 * verifica qual banco de dados está sendo utilizado e
	 * fornece o acesso. <br>
	 * Somente é possível conexões com bancos de dados MySQL e H2.
	 */
	protected DatabaseConnection() {
		
	}
	
	/**
	 * Cria uma nova instância para a classe.
	 * 
	 * @return objDatabaseConnection
	 */
	public static synchronized DatabaseConnection getInstance() {
		//String strDatabase = LTParameters.getInstance().getDatabase();
		String strDatabase = "MySQL";
		
		if (strDatabase.equals("MySQL")) {
			objDatabaseConnection = new DatabaseMySQLConnection();
		} else if (strDatabase.equals("H2")) {
			objDatabaseConnection = new DatabaseH2Connection();
		}
		
		return objDatabaseConnection;
	}
	
	/**
	 * Altera os dados da conexão do banco de dados.
	 * 
	 * @param strDatabaseServer - Servidor do banco de dados
	 * @param strDatabaseName   - Nome do banco de dados
	 * @param strDatabaseUser   - Usuário do banco de dados
	 * @param strDatabasePass   - Senha de acesso ao banco de dados
	 */
	public void setDatabaseConnection(String strDatabaseServer, String strDatabaseName, String strDatabaseUser, String strDatabasePass) {
		
	}
	
	/**
	 * Abre a conexão com o banco de dados.
	 */
	public void openConnection() {
		
	}
	
	/**
	 * Fecha a conexão com o banco de dados.
	 */
	public void closeConnection() {
		
	}
	
	/**
	 * Persiste as transações e fecha a conexão com o banco de dados.
	 */
	public void commitTransaction() {
		
	}
	
	/**
	 * Reverte as transações e fecha a conexão com o banco de dados.
	 */
	public void rollBackTransaction() {
		
	}
	
	/**
	 * Inicializa uma nova transação.
	 */
	public void initiliazeStatement() {
		
	}
	
	/**
	 * Armazena uma instrução SQL na variável <i>strSQLCommand</i>
	 * 
	 * @param strSQLCommand
	 */
	public void sqlCommand(String strSQLCommand) {
		
	}
	
	/**
	 * Concatena uma instrução SQL na variável <i>strSQLCommand</i>
	 * 
	 * @param strSQLCommandAppend
	 */
	public void sqlCommandAppend(String strSQLCommandAppend) {
		
	}
	
	/**
	 * Adiciona um parâmetro à transação.
	 * 
	 * @param strColumnDatabase - Coluna do banco de dados
	 * @param objDataType       - Tipo de dado
	 * @param objValue          - Valor do parâmetro
	 */
	public void addParameter(String strColumnDatabase, LTDataTypes objDataType, Object objValue) {
		
	}
	
	/**
	 * Executa uma <i>SQL Query (INSERT, UPDATE, DELETE)</i> 
	 * de adição, atualização ou exclusão de dados.
	 * 
	 * @return intRecordsAffected - Número de registros processados
	 */
	public int executeQuery() throws SQLException {
		return 0;
	}
	
	/**
	 * Executa uma <i>SQL Query (SELECT)</i> 
	 * de consulta de dados. 
	 * 
	 * @return rsReturn - ResultSet
	 */
	public ResultSet executeSelectQuery() throws SQLException {
		return null;
	}
	
	/**
	 * Cancela a execução de uma consulta em andamento.
	 */
	public void cancelSelectQuery() {
		
	}
	
	/**
	 * Retorna o valor do primeiro resultado 
	 * do primeiro campo do método <i>executeSelectQuery</i>.
	 * 
	 * @param objType - Tipo de valor que queremos que seja retornado
	 * 
	 * @return objValue
	 */
	public Object getFirstValue(LTDataTypes objDataType) {
		return null;
	}
	
	/**
	 * Retorna o ID do registro pré-gravado no banco de dados.
	 * 
	 * @return lgnIdentityKey
	 */
	public long getIdentityKey() {
		return 0;
	}
	
	/**
	 * Retorna a quantidade de registros de uma consulta.
	 * 
	 * @return intTotalRecords
	 */
	public int getTotalRecords() {
		return 0;
	}
}