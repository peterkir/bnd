# Sample BND Workspace

This is a sample workspace demonstrating all major features of the **bnd CLI** tool.
It is designed to be used as test data for:

- Resolving OSGi bundles
- Executing bnd and bndrun files
- Running test cases (JUnit and OSGi tests)
- Debugging OSGi applications

## Workspace Structure

```
sample-workspace/
├── cnf/                       # Workspace configuration
│   ├── build.bnd              # Main build settings
│   ├── ext/                   # Extensions and repository config
│   │   ├── repositories.bnd   # Repository definitions
│   │   ├── junit.bnd          # JUnit configuration
│   │   └── central.mvn        # Maven Central index
│   ├── includes/              # Shared includes
│   │   └── jdt.bnd            # JDT settings bridge
│   └── repo/                  # Local repository
│       └── index.xml          # Repository index
├── sample.api/                # API bundle (service interfaces)
│   ├── bnd.bnd
│   └── src/sample/api/
│       ├── SampleService.java
│       └── package-info.java
├── sample.impl/               # Implementation bundle (DS components)
│   ├── bnd.bnd
│   └── src/sample/impl/
│       ├── Activator.java
│       └── DefaultSampleServiceImpl.java
├── sample.test/               # Test bundle with various bndrun files
│   ├── bnd.bnd
│   ├── src/sample/test/       # OSGi test cases
│   │   └── SampleServiceTest.java
│   ├── test/sample/test/      # Non-OSGi JUnit tests
│   │   └── SimpleTest.java
│   ├── simple.bndrun          # Simple run configuration
│   ├── debug.bndrun           # Debug configuration with JVM options
│   ├── testrun.bndrun         # OSGi test configuration
│   ├── resolve-example.bndrun # Resolver demonstration
│   ├── export.bndrun          # Executable JAR export
│   └── framework-restart.bndrun # Framework persistence options
└── README.md                  # This file
```

## Prerequisites

- Java 17 or higher
- bnd CLI tool (install via Homebrew, SDKMAN, or download from https://bnd.bndtools.org)

## Quick Start

### 1. Navigate to the workspace

```bash
cd sample-workspace
```

### 2. Build all projects

```bash
# Build the entire workspace
bnd build

# Build a specific project
bnd build -P sample.api
bnd build -P sample.impl
bnd build -P sample.test

# Continuous/watch mode
bnd build -w

# Force rebuild (non-incremental)
bnd build -f
```

### 3. View project information

```bash
# Show project details
bnd project -p sample.impl

# Show build path
bnd info -b -p sample.impl

# Show debug information
bnd debug -p sample.impl
```

## Running OSGi Applications

### Simple Run

```bash
# Run the simple configuration
bnd run sample.test/simple.bndrun

# Run with verification
bnd run -v sample.test/simple.bndrun
```

### Debug Run

```bash
# Run with remote debugging enabled
bnd run sample.test/debug.bndrun

# Then attach a debugger to port 5005
# In Eclipse: Debug Configurations > Remote Java Application
```

## Testing

### Run OSGi Tests

```bash
# Run tests from project bnd.bnd
bnd test -p sample.test

# Run tests from bndrun file
bnd test sample.test/testrun.bndrun

# Run with trace output
bnd test -t -p sample.test

# Run specific test class
bnd test sample.test.SampleServiceTest -p sample.test

# Run specific test method
bnd test sample.test.SampleServiceTest:testGreet -p sample.test
```

### Run JUnit Tests (Non-OSGi)

```bash
# Run plain JUnit tests
bnd junit -p sample.test

# Run with verbose output
bnd junit -v -p sample.test

# Run specific test
bnd junit sample.test.SimpleTest -p sample.test
```

### Generate Test Reports

```bash
# Run tests with HTML report
bnd runtests sample.test/testrun.bndrun

# Custom report directory and title
bnd runtests -r test-reports -T "Sample Tests" sample.test/testrun.bndrun
```

## Resolving Dependencies

### Basic Resolution

```bash
# Resolve a bndrun file (show results)
bnd resolve sample.test/resolve-example.bndrun

# Show resolved bundles
bnd resolve -b sample.test/resolve-example.bndrun

# Show file paths
bnd resolve -f sample.test/resolve-example.bndrun

# Show URLs
bnd resolve -u sample.test/resolve-example.bndrun
```

### Update bndrun File

```bash
# Write resolved -runbundles back to the file
bnd resolve -W sample.test/resolve-example.bndrun

# Fail if changes are made (useful for CI)
bnd resolve -x sample.test/resolve-example.bndrun
```

### Create Dependency Graph

```bash
# Create a DOT file for visualization
bnd resolve -d sample.test/simple.bndrun

# Named output file
bnd resolve dot -o dependencies.dot sample.test/simple.bndrun

# View with GraphViz
dot -Tpng dependencies.dot -o dependencies.png
```

## Exporting

### Create Executable JAR

```bash
# Export to executable JAR
bnd export sample.test/export.bndrun

# Custom output location
bnd export -o myapp.jar sample.test/export.bndrun

# Verbose output
bnd export -v sample.test/export.bndrun
```

### Run Exported JAR

```bash
# The exported JAR is self-contained
java -jar generated/sample-app.jar
```

## Repository Commands

### List Bundles

```bash
# List all bundles in repositories
bnd repo list

# List specific bundle versions
bnd repo versions org.apache.felix.framework

# Query bundles
bnd repo list -q "felix"
```

### Repository Information

```bash
# Show available repositories
bnd repo repos

# Refresh repository indexes
bnd repo refresh
```

## Cleaning

```bash
# Clean a project
bnd clean -p sample.impl

# Clean the entire workspace
bnd clean -w .

# Verbose output
bnd clean -v -p sample.impl
```

## Eclipse / Bndtools Integration

This workspace is fully compatible with Eclipse and Bndtools:

1. **Import Workspace**: File > Import > Existing Projects into Workspace
2. **Select folder**: `sample-workspace`
3. **Import all projects**: cnf, sample.api, sample.impl, sample.test

### Running in Eclipse

- **Run**: Right-click `.bndrun` file > Run As > Bnd OSGi Run Launcher
- **Debug**: Right-click `.bndrun` file > Debug As > Bnd OSGi Run Launcher
- **Test**: Right-click `.bndrun` file > Run As > Bnd OSGi Test Launcher

### Resolving in Eclipse

1. Open a `.bndrun` file
2. Click "Resolve" in the Run tab
3. Review resolved bundles
4. Click "Update" to save changes

## Common Options

Most bnd commands support these options:

| Option | Description |
|--------|-------------|
| `-p, --project <path>` | Specify project directory |
| `-w, --workspace <path>` | Specify workspace directory |
| `-v, --verbose` | Verbose output |
| `-e, --exclude <pattern>` | Exclude files by pattern |
| `-t, --trace` | Enable tracing |

## Troubleshooting

### View All Properties

```bash
bnd debug -f -p sample.impl
```

### Check Build Path

```bash
bnd info -b -p sample.impl
```

### Verify Dependencies

```bash
bnd run -v sample.test/simple.bndrun
```

### Show Macro Expansion

```bash
bnd macro "workspace: ${workspace}" -p sample.impl
```

## Additional Commands

### Print JAR Information

```bash
bnd print sample.api/generated/sample.api.jar
```

### Diff JARs

```bash
bnd diff old.jar new.jar
```

### Baseline (API Compatibility)

```bash
bnd baseline sample.api/generated/sample.api.jar baseline.jar
```

## Resources

- **Documentation**: https://bnd.bndtools.org
- **Bnd CLI Commands**: https://bnd.bndtools.org/chapters/100-bnd-commands.html
- **GitHub**: https://github.com/bndtools/bnd
- **Discourse**: https://bnd.discourse.group

## License

This sample workspace is part of the bnd/bndtools project and is licensed under the same terms.
