CREATE TABLE ap_types (
    tp_id            INT IDENTITY(1,1) PRIMARY KEY,
    tp_name          NVARCHAR(32) NOT NULL,
    tp_size          INT NOT NULL CONSTRAINT DF_ap_types_tp_size DEFAULT 0,
    tp_key           VARBINARY(255) NULL,
    tp_color         NVARCHAR(32) NULL,
    tp_date           DATE NULL,
    tp_datetime       DATETIME NULL,
    tp_datetime2      DATETIME2(6) NULL,
    tp_datetimeoffset DATETIMEOFFSET(6) NULL,
    tp_smalldatetime  SMALLDATETIME NULL,
    tp_time           TIME(6) NULL,
    tp_time_created  DATETIME2(6) NOT NULL CONSTRAINT DF_ap_types_created
                         DEFAULT SYSUTCDATETIME(),
    tp_time_updated  DATETIME2(6) NOT NULL CONSTRAINT DF_ap_types_updated
                         DEFAULT SYSUTCDATETIME()
);
