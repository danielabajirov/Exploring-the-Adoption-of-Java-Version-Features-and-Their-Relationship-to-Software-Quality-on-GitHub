import csv
import re
import pandas as pd

def process_csv(input_methods, mapping_file, input_file_java):
    with open(input_file_java, 'r') as f, open(input_methods, 'r') as methods_file:
        java_input_rows = list(csv.DictReader(f, delimiter=';'))
        output_reader = csv.DictReader(methods_file, delimiter=',')
        output_writer = setup_output_writer(mapping_file)

        for row in output_reader:
            process_row(row, output_writer, java_input_rows)

def setup_output_writer(mapping_file):
    output_file = open(mapping_file, 'a', newline='')
    writer = csv.DictWriter(output_file, fieldnames=['Repository', 'Method', 'Count', 'Imports', 'Version', 'MethodMatched', 'Href', 'VersionWithImport', 'Import', 'HrefMethodMatched', 'Annotations'], delimiter=';')
    writer.writeheader()
    return writer

def process_row(row, output_writer, java_input_rows):
    try:
        method_info = match_method(row, java_input_rows)
        output_writer.writerow(method_info)
        print("Writing version: " + method_info['Version'])
    except Exception as e:
        print(f"An error occurred while processing row {row}: {str(e)}")

def match_method(row, java_input_rows):
    method = row['Method'] + '()'
    output_method_parameters = row['Parameters'][1:-1].split(';')
    
    for input_row in java_input_rows:
        modified_value, input_method_parameters = preprocess_input_row(input_row)
        
        if method == modified_value and set(output_method_parameters) == set(input_method_parameters):
            return extract_method_info(row, input_row)
    return construct_default_method_info(row)

def preprocess_input_row(input_row):
    modified_value = re.sub(r'\(.*\)', '()', input_row['Value']).strip()
    input_method_parameters_str = re.search(r'\((.*?)\)', input_row['Value']).group(1) if re.search(r'\((.*?)\)', input_row['Value']) else ''
    input_method_parameters = input_method_parameters_str.replace(" ", "").split(',')
    return modified_value, input_method_parameters




def run_actions():
    join_method_panda_one_file('D:/InformationDataMining/java_code_analysis_results_15_July_2022(3)_part3.csv', './java_methods_version8_mapped_final(2).csv', 1)
    
    time.sleep(10)
    split_file('D:/InformationDataMining/final_information_repositories_merged_version8_15_July_2022(3)_part3.csv', 150)
    time.sleep(10)

    for i in range(1, 151):
        input_file = f'D:/InformationDataMining/final_information_repositories_merged_version8_15_July_2022(3)_part3.csv_part{i}.csv'
        output_file = f'D:/InformationDataMining/final_information_repositories_merged_version8_15_July_2022(3)_part{i}_final_results_part1.csv'
        analyze(input_file, output_file)
        
# run_actions()        
        
def process_part(part_number):
    input_file = f'D:/InformationDataMining/java_code_analysis_results.csv_part{part_number}.csv'
    output_file = f'D:/InformationDataMining/java_code_analysis_results_version_8_part{part_number}.csv'
    mapping_file = 'C:/Users/abaji/OneDrive/Desktop/Masterarbeit/java_methods_version8(2).csv'  
     
    try:
        process_csv_methods(input_file, output_file, mapping_file)
        print(f'Successfully processed part {part_number}')
    except Exception as e:
        print(f"An error occurred while processing part {part_number}: {str(e)}")