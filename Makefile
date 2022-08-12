CORE_NAME = ap_core

.PHONY: build clean

build:
	cd src/fpga; quartus_sh --flow compile $(CORE_NAME)
	bin/reverse src/fpga/output_files/$(CORE_NAME).rbf dist/$(CORE_NAME).rbf_r

clean:
	rm -rf src/fpga/db src/fpga/incremental_db src/fpga/output_files dist/$(CORE_NAME).rbf_r
