from github import Github
import csv
import datetime
import re

# Authentication and Configuration
ACCESS_TOKEN = "your_personal_access_token"  # Replace with your actual access token
BASE_QUERY = "language:java stars:>100"
CSV_FILE_PATH = 'github_repos_mining.csv'
EXCEPTION_FILE_PATH = 'github_repos_exceptions.csv'
JAVA_PERCENTAGE_THRESHOLD = 75

# Initialize GitHub API with an access token
g = Github(ACCESS_TOKEN)

def write_to_csv(writer, data):
    """Write a list of data to the provided CSV writer."""
    writer.writerow(data)

def get_java_percentage(repo_languages):
    """Calculate the percentage of Java code in the repository."""
    total = sum(repo_languages.values())
    java_lines = repo_languages.get('Java', 0)
    return (java_lines / total) * 100 if total > 0 else 0

def is_excluded_based_on_description(description):
    """Check if the repository should be excluded based on its description."""
    if description:
        description_lower = description.lower()
        if "collection" in description_lower or "interview" in description_lower:
            return True
        if len(re.findall(r'[\u4e00-\u9fff]', description)) >= 2:
            return True
    return False

def repo_recently_pushed(repo):
    """Check if the repository had any pushes in the last six months."""
    return repo.pushed_at >= datetime.datetime.now() - datetime.timedelta(days=180)

# Main data collection and processing
with open(CSV_FILE_PATH, 'a', newline='') as file, open(EXCEPTION_FILE_PATH, 'a', newline='') as exception_file:
    writer = csv.writer(file)
    exception_writer = csv.writer(exception_file)
    # CSV headers
    writer.writerow(["Name", "URL", "Stars", "Forks", "Last Commit"])
    exception_writer.writerow(["Name", "URL", "Error"])

    try:
        repos = g.search_repositories(query=BASE_QUERY)
        for repo in repos:
            if repo_recently_pushed(repo) and not is_excluded_based_on_description(repo.description):
                try:
                    languages = repo.get_languages()
                    java_percent = get_java_percentage(languages)
                    if java_percent >= JAVA_PERCENTAGE_THRESHOLD:
                        repo_data = [repo.name, repo.html_url, repo.stargazers_count, repo.forks, repo.pushed_at]
                        write_to_csv(writer, repo_data)
                except Exception as e:
                    # Rate limit exceptions and other exceptions are handled here
                    exception_data = [repo.name, repo.html_url, str(e)]
                    write_to_csv(exception_writer, exception_data)
    except GithubException as ghe:
        print(f"GitHub API exception occurred: {ghe}")
    except Exception as e:
        print(f"An error occurred: {e}")
