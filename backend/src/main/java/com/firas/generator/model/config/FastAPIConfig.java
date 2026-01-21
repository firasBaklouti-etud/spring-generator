package com.firas.generator.model.config;

/**
 * FastAPI (Python) specific configuration for project generation.
 * 
 * Contains Python and FastAPI-specific settings that don't apply
 * to other technology stacks.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-07
 */
public class FastAPIConfig {
    
    /** Python version to use (e.g., "3.10", "3.11", "3.12") */
    private String pythonVersion = "3.11";
    
    /** Package manager: "pip", "poetry", "pipenv" */
    private String packageManager = "pip";
    
    /** ORM to use: "sqlalchemy", "tortoise", "sqlmodel" */
    private String orm = "sqlalchemy";
    
    /** FastAPI version (e.g., "0.109.0") */
    private String fastapiVersion = "0.109.0";
    
    /** Whether to use async/await patterns */
    private boolean useAsync = true;
    
    /** Whether to include Pydantic v2 for validation */
    private boolean usePydantic = true;
    
    /** Whether to use Alembic for database migrations */
    private boolean useAlembic = true;
    
    /** Whether to include pytest for testing */
    private boolean usePytest = true;
    
    /** Whether to generate Docker/docker-compose files */
    private boolean useDocker = false;

    // Constructors
    public FastAPIConfig() {}

    // Getters and Setters
    public String getPythonVersion() { return pythonVersion; }
    public void setPythonVersion(String pythonVersion) { this.pythonVersion = pythonVersion; }

    public String getPackageManager() { return packageManager; }
    public void setPackageManager(String packageManager) { this.packageManager = packageManager; }

    public String getOrm() { return orm; }
    public void setOrm(String orm) { this.orm = orm; }

    public String getFastapiVersion() { return fastapiVersion; }
    public void setFastapiVersion(String fastapiVersion) { this.fastapiVersion = fastapiVersion; }

    public boolean isUseAsync() { return useAsync; }
    public void setUseAsync(boolean useAsync) { this.useAsync = useAsync; }

    public boolean isUsePydantic() { return usePydantic; }
    public void setUsePydantic(boolean usePydantic) { this.usePydantic = usePydantic; }

    public boolean isUseAlembic() { return useAlembic; }
    public void setUseAlembic(boolean useAlembic) { this.useAlembic = useAlembic; }

    public boolean isUsePytest() { return usePytest; }
    public void setUsePytest(boolean usePytest) { this.usePytest = usePytest; }

    public boolean isUseDocker() { return useDocker; }
    public void setUseDocker(boolean useDocker) { this.useDocker = useDocker; }
}
