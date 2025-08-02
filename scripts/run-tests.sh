#!/bin/bash

# Bubble Timer Test Execution Script
# This script provides different test execution options for the project

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Bubble Timer Test Execution Script"
    echo ""
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  unit                    Run unit tests only (fast)"
    echo "  integration             Run integration tests only (medium)"
    echo "  all                     Run all tests (default)"
    echo "  coverage                Run tests with coverage report"
    echo "  clean                   Clean build and run all tests"
    echo "  debug                   Run tests with debug output"
    echo "  help                    Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 unit                 # Run only unit tests"
    echo "  $0 integration          # Run only integration tests"
    echo "  $0 coverage            # Run tests with coverage"
    echo "  $0 clean               # Clean build and run all tests"
}

# Function to run unit tests
run_unit_tests() {
    print_status "Running unit tests..."
    
    # Run unit tests with specific patterns
    ./gradlew testDebugUnitTest \
        --tests="io.jhoyt.bubbletimer.core.*" \
        --tests="io.jhoyt.bubbletimer.util.*" \
        --tests="io.jhoyt.bubbletimer.websocket.WebsocketManagerOnDemandTest" \
        --tests="io.jhoyt.bubbletimer.websocket.WebsocketManagerMessageTest" \
        --tests="io.jhoyt.bubbletimer.service.ForegroundServiceTest" \
        --tests="io.jhoyt.bubbletimer.sharing.SharedTimerPersistenceTest" \
        --tests="io.jhoyt.bubbletimer.repository.ActiveTimerRepositoryTest" \
        --tests="io.jhoyt.bubbletimer.websocket.WebsocketManagerConnectionTest" \
        --tests="io.jhoyt.bubbletimer.util.TestDataFactoryTest" \
        --tests="io.jhoyt.bubbletimer.di.DependencyInjectionTest"
    
    print_success "Unit tests completed!"
}

# Function to run integration tests
run_integration_tests() {
    print_status "Running integration tests..."
    
    # Run integration tests (both unit test integration and android test integration)
    ./gradlew testDebugUnitTest \
        --tests="io.jhoyt.bubbletimer.repository.*" \
        --tests="io.jhoyt.bubbletimer.websocket.WebsocketManagerConnectionTest"
    
    print_success "Integration tests completed!"
}

# Function to run all tests
run_all_tests() {
    print_status "Running all tests..."
    
    ./gradlew test
    
    print_success "All tests completed!"
}

# Function to run tests with coverage
run_coverage_tests() {
    print_status "Running tests with coverage..."
    
    # Create coverage directory if it doesn't exist
    mkdir -p build/reports/coverage
    
    # Run tests with coverage
    ./gradlew testDebugUnitTest \
        --tests="io.jhoyt.bubbletimer.core.*" \
        --tests="io.jhoyt.bubbletimer.util.*" \
        --tests="io.jhoyt.bubbletimer.websocket.*" \
        --tests="io.jhoyt.bubbletimer.service.*" \
        --tests="io.jhoyt.bubbletimer.sharing.*" \
        --tests="io.jhoyt.bubbletimer.repository.*" \
        --tests="io.jhoyt.bubbletimer.integration.*" \
        --tests="io.jhoyt.bubbletimer.di.*"
    
    print_success "Coverage tests completed!"
    print_status "Coverage report available at: build/reports/tests/testDebugUnitTest/index.html"
}

# Function to clean and run tests
run_clean_tests() {
    print_status "Cleaning build and running tests..."
    
    ./gradlew clean
    ./gradlew test
    
    print_success "Clean build and tests completed!"
}

# Function to run tests with debug output
run_debug_tests() {
    print_status "Running tests with debug output..."
    
    ./gradlew testDebugUnitTest --info --stacktrace
    
    print_success "Debug tests completed!"
}

# Function to check test results
check_test_results() {
    local test_type=$1
    
    if [ $? -eq 0 ]; then
        print_success "$test_type tests passed!"
    else
        print_error "$test_type tests failed!"
        exit 1
    fi
}

# Main execution
case "${1:-all}" in
    "unit")
        run_unit_tests
        check_test_results "Unit"
        ;;
    "integration")
        run_integration_tests
        check_test_results "Integration"
        ;;
    "all")
        run_all_tests
        check_test_results "All"
        ;;
    "coverage")
        run_coverage_tests
        check_test_results "Coverage"
        ;;
    "clean")
        run_clean_tests
        check_test_results "Clean"
        ;;
    "debug")
        run_debug_tests
        check_test_results "Debug"
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        print_error "Unknown option: $1"
        show_usage
        exit 1
        ;;
esac 