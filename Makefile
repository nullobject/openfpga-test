.PHONY: build clean

build:
	cd src/fpga; quartus_sh --flow compile ap_core

clean:
	rm -rf out src/fpga/output_files
