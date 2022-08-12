CORE_NAME = ap_core

.PHONY: build clean

build:
	cd quartus; quartus_sh --flow compile $(CORE_NAME)
	bin/reverse quartus/output_files/$(CORE_NAME).rbf dist/$(CORE_NAME).rbf_r

clean:
	rm -rf dist/$(CORE_NAME).rbf_r quartus/db quartus/incremental_db quartus/output_files
