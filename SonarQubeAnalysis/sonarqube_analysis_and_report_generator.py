import requests
import os
import subprocess
import csv
from requests.auth import HTTPBasicAuth
import base64

# Constants
SONAR_URL = "http://localhost:9000"
AUTH = ('admin', 'admin')  # SonarQube login credentials
AUTH_TOKEN = "sqp_0ee54e88d144abfe195f72417d3a5ecf3728a7f1"  # Replace with your actual token
METRICS = ['bugs', 'code_smells', 'vulnerabilities', 'security_hotspots_reviewed']
REPOSITORY_PATH = "D:/Repositories"
RESULTS_DIR = "D:/SonarResults"

# Ensure the results directory exists
if not os.path.exists(RESULTS_DIR):
    os.mkdir(RESULTS_DIR)

def encode_credentials(username, password):
    """Encodes credentials for Basic Auth."""
    credentials = f"{username}:{password}"
    return base64.b64encode(credentials.encode('utf-8')).decode('utf-8')

def get_auth_headers(token=None, username=None, password=None):
    """Returns the appropriate headers for authentication."""
    if token:
        return {'Authorization': f'Bearer {token}'}
    elif username and password:
        encoded_credentials = encode_credentials(username, password)
        return {'Authorization': f'Basic {encoded_credentials}'}
    else:
        return {}

def fetch_project_issues(sonar_host, project_key, metrics, auth_token):
    """Fetches issues for the project from SonarQube."""
    issues = {}
    for metric in metrics:
        response = requests.get(
            f"{sonar_host}/api/issues/search",
            params={
                'componentKeys': project_key,
                'types': metric,
                'ps': 500
            },
            headers=get_auth_headers(token=auth_token)
        )
        if response.status_code == 200:
            issues[metric] = response.json().get('issues', [])
        else:
            print(f"Failed to fetch issues for metric {metric}. Status Code: {response.status_code}")
    return issues

def run_sonar_scanner(project_path, project_key, sonar_host, sonar_login_token):
    """Runs the Sonar Scanner on a given project."""
    compile_command = "mvn compile"
    analysis_command = f"mvn sonar:sonar -Dsonar.projectKey={project_key} -Dsonar.host.url={sonar_host} -Dsonar.login={sonar_login_token} -Dsonar.java.binaries=target/classes"

    try:
        subprocess.check_output(compile_command, cwd=project_path, shell=True)
        subprocess.check_output(analysis_command, cwd=project_path, shell=True)
        print(f"SonarQube analysis completed for project: {project_key}")
    except subprocess.CalledProcessError as e:
        print(f"Command failed: {e.cmd}")
        print(f"Output: {e.output}")

def write_issues_to_csv(output_file, project_path, project_key, issues):
    """Writes the issues to a CSV file."""
    with open(output_file, 'a', newline='') as f:
        writer = csv.writer(f)
        for metric, issue_list in issues.items():
            for issue in issue_list:
                writer.writerow([project_path, project_key, metric, issue['message'], issue['severity']])

# Example usage
project_path = "/path/to/your/java/project"
project_key = "Java-repos"
output_file = f"{RESULTS_DIR}/sonar_results.csv"

# Run the scanner (uncomment the next line to actually run it)
# run_sonar_scanner(project_path, project_key, SONAR_URL, AUTH_TOKEN)

# Fetch and write issues to CSV
issues = fetch_project_issues(SONAR_URL, project_key, METRICS, AUTH_TOKEN)
write_issues_to_csv(output_file, project_path, project_key, issues)
