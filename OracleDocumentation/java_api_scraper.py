import requests
from bs4 import BeautifulSoup
import pandas as pd
from urllib.parse import urlparse

def extract_method_data(base_url, index_range):
    """
    Extracts method names and their hrefs from the Java Version, for example SE 8 API documentation index pages.
    
    Args:
        base_url (str): The base URL for the Java Version, for example SE 8 API documentation.
        index_range (range): A range of index numbers corresponding to the API index pages.
    
    Returns:
        list of lists: A list containing sublists with the method name and href for each method.
    """
    all_data = []

    # Loop through the specified range of index pages
    for i in index_range:
        url = f"{base_url}index-files/index-{i}.html"
        print(f"Processing {url}")
        try:
            response = requests.get(url)
            soup = BeautifulSoup(response.content, 'html.parser')

            dl_elements = soup.find_all('dl')
            for dl in dl_elements:
                dt_elements = dl.find_all('dt')
                for dt in dt_elements:
                    a_element = dt.find('a')
                    if a_element:
                        href = a_element['href'].replace('../', '/')
                        full_href = base_url + href
                        method_name = a_element.text.strip()
                        all_data.append([method_name, full_href])
        except Exception as e:
            print(f"An error occurred while processing {url}: {e}")
    
    return all_data

def extract_version(href):
    """
    Extracts the version information from the given href of a Java Version, for example SE 8 API method.
    
    Args:
        href (str): The href URL containing the Java method details.
    
    Returns:
        str: The version information extracted from the href page. If not found, returns 'VERSION NOT FOUND'.
    """
    try:
        response = requests.get(href)
        soup = BeautifulSoup(response.text, 'html.parser')
        parsed_url = urlparse(href)
        fragment = parsed_url.fragment

        # Attempt to locate the 'Since' label within different parts of the parsed HTML
        search_areas = [
            soup.find('a', name=fragment).find_next('ul', class_='blockList') if soup.find('a', name=fragment) else None,
            soup.find('div', class_='description')
        ]

        for area in search_areas:
            if area:
                since_label = area.find('span', class_='simpleTagLabel', text='Since:')
                if since_label:
                    version = since_label.find_next('dd').text
                    return version.strip()

        return "VERSION NOT FOUND"
    except Exception as e:
        print(f"An error occurred while extracting version from {href}: {e}")
        return "ERROR"

# Define the base URL for the Java Version, for example SE 8 API documentation
base_url = "https://docs.oracle.com/javase/8/docs/api/"

# Define the range of index pages to process
index_range = range(1, 28)

# Extract the methods and hrefs from the index pages
methods_data = extract_method_data(base_url, index_range)

# Convert the data into a pandas DataFrame and save to CSV
df_methods = pd.DataFrame(methods_data, columns=['Method', 'Href'])
df_methods.to_csv('java_methods_version8.csv', index=False, sep=';')
print("Data extraction complete. CSV file created.")
