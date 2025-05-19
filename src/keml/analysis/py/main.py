import workbook_analyser
import workbook_editor
import os
import sys

DEFAULT_PATH = "./../keml.sample/introductoryExamples/analysis"


def main():
    PATH = DEFAULT_PATH
    if len(sys.argv) < 2:
        raise TypeError("missing path argument")
    elif len(sys.argv) == 2:       
        dir_name = sys.argv[1]
    else:
        dir_name = sys.argv[1]
        PATH = sys.argv[2]
    user_dir_path = f'{PATH}/{dir_name}'
    if os.path.isdir(user_dir_path):
        if not os.path.exists(f'{user_dir_path}/stats'):
            os.mkdir(f'{user_dir_path}/stats')
        for ana_file in os.listdir(user_dir_path):     
            if os.path.isdir(f'{user_dir_path}/{ana_file}') or os.path.splitext(ana_file)[1] == ".csv":
                continue
            f_s = os.path.join(user_dir_path, ana_file)          
            h_t = f'{user_dir_path}/stats/{os.path.splitext(ana_file)[0]}'
            workbook_editor.insert_rand_values(f_s)
            workbook_analyser.run_wb_analysis(f_s, h_t)

if __name__ == '__main__':
	main()
