package dev.mfarm.com.mfarm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import java.io.File;

import dev.mfarm.com.mfarm.sync.FarmIdentity;
import dev.mfarm.com.mfarm.sync.FarmNetwork;
import dev.mfarm.com.mfarm.sync.FarmSyncManager;

public class FarmSyncActivity extends AppCompatActivity {
    private static final int REQ_FOLDER = 41;
    private static final int REQ_IMPORT = 42;
    private static final int REQ_JOIN_IMPORT = 43;

    private TextView tvStatus;
    private TextView tvFarm;
    private TextView tvCode;
    private Button btnCreate;
    private Button btnJoin;
    private Button btnShareCode;
    private Button btnSendBackup;
    private Button btnImport;
    private Button btnFolder;
    private Button btnSyncNow;
    private String pendingJoinCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_sync);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Farm Sync");
        }

        tvStatus = findViewById(R.id.tvSyncStatus);
        tvFarm = findViewById(R.id.tvFarmName);
        tvCode = findViewById(R.id.tvJoinCode);
        btnCreate = findViewById(R.id.btnCreateFarm);
        btnJoin = findViewById(R.id.btnJoinFarm);
        btnShareCode = findViewById(R.id.btnShareCode);
        btnSendBackup = findViewById(R.id.btnSendBackup);
        btnImport = findViewById(R.id.btnImportBackup);
        btnFolder = findViewById(R.id.btnPickFolder);
        btnSyncNow = findViewById(R.id.btnSyncNow);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptCreateFarm();
            }
        });
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptJoinFarm();
            }
        });
        btnShareCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareInvite();
            }
        });
        btnSendBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBackup();
            }
        });
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!FarmIdentity.isSet(FarmSyncActivity.this)) {
                    Toast.makeText(FarmSyncActivity.this, "Create or join a farm first", Toast.LENGTH_LONG).show();
                    return;
                }
                pickBackup(REQ_IMPORT);
            }
        });
        btnFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!FarmIdentity.isSet(FarmSyncActivity.this)) {
                    Toast.makeText(FarmSyncActivity.this, "Create or join a farm first", Toast.LENGTH_LONG).show();
                    return;
                }
                pickFolder();
            }
        });
        btnSyncNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSyncNow();
            }
        });

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        FarmIdentity identity = FarmIdentity.get(this);
        tvStatus.setText(FarmSyncManager.bannerText(this));
        boolean ready = identity != null;
        btnShareCode.setEnabled(ready);
        btnSendBackup.setEnabled(ready);
        btnImport.setEnabled(ready);
        btnFolder.setEnabled(ready);
        btnSyncNow.setEnabled(ready && FarmIdentity.folderUri(this) != null);
        if (identity == null) {
            tvFarm.setText("This phone is solo. Create a farm to invite someone, or join with a code.");
            tvCode.setText("———");
        } else {
            tvFarm.setText(identity.farmName + " · you are the " + identity.role);
            tvCode.setText(FarmIdentity.formatJoinCode(identity.joinCode));
        }
    }

    private void promptCreateFarm() {
        if (FarmIdentity.isSet(this)) {
            Toast.makeText(this, "This phone already belongs to a farm.", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("Farm name");
        new AlertDialog.Builder(this)
                .setTitle("Create this farm")
                .setMessage("Anyone with the code can merge records onto this herd. Keep entering data with no signal.")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FarmIdentity.create(FarmSyncActivity.this, input.getText().toString());
                        Toast.makeText(FarmSyncActivity.this,
                                "Farm created. Share the code, then send a backup or pick a shared Drive folder.",
                                Toast.LENGTH_LONG).show();
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptJoinFarm() {
        final EditText input = new EditText(this);
        input.setHint("Code like K7P2-QM4N");
        new AlertDialog.Builder(this)
                .setTitle("Join a farm")
                .setMessage("Enter the code from the other phone. Next you will import their backup or open the same shared folder.")
                .setView(input)
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String code = input.getText().toString();
                        if (!FarmIdentity.isValidJoinCode(code)) {
                            Toast.makeText(FarmSyncActivity.this, "That code does not look right", Toast.LENGTH_LONG).show();
                            return;
                        }
                        pendingJoinCode = FarmIdentity.normalizeJoinCode(code);
                        new AlertDialog.Builder(FarmSyncActivity.this)
                                .setTitle("How should we get the records?")
                                .setItems(new CharSequence[]{
                                        "Import a backup file",
                                        "Open a shared Drive folder"
                                }, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int whichItem) {
                                        if (whichItem == 0) {
                                            pickBackup(REQ_JOIN_IMPORT);
                                        } else {
                                            pickFolder();
                                        }
                                    }
                                })
                                .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareInvite() {
        FarmIdentity identity = FarmIdentity.get(this);
        if (identity == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Join my farm on MFarm");
        intent.putExtra(Intent.EXTRA_TEXT, identity.shareMessage());
        startActivity(Intent.createChooser(intent, "Send farm code"));
    }

    private void sendBackup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File file = FarmSyncManager.exportToCache(FarmSyncActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Uri uri = FileProvider.getUriForFile(
                                    FarmSyncActivity.this,
                                    "dev.mfarm.com.mfarm.fileprovider",
                                    file);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/octet-stream");
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.putExtra(Intent.EXTRA_SUBJECT, "MFarm backup");
                            FarmIdentity identity = FarmIdentity.get(FarmSyncActivity.this);
                            intent.putExtra(Intent.EXTRA_TEXT, identity != null ? identity.shareMessage() : "");
                            startActivity(Intent.createChooser(intent, "Send farm backup"));
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FarmSyncActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void pickBackup(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_FOLDER);
    }

    private void runSyncNow() {
        tvStatus.setText(FarmNetwork.statusLabel(this) + " · Syncing…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String msg = FarmSyncManager.autoSync(FarmSyncActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FarmSyncActivity.this, msg, Toast.LENGTH_LONG).show();
                        refresh();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        final Uri uri = data.getData();
        if (requestCode == REQ_FOLDER) {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (pendingJoinCode != null && !FarmIdentity.isSet(this)) {
                final String code = pendingJoinCode;
                pendingJoinCode = null;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final FarmSyncManager.SyncResult result =
                                    FarmSyncManager.joinFromFolder(FarmSyncActivity.this, code, uri);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(FarmSyncActivity.this, result.message, Toast.LENGTH_LONG).show();
                                    refresh();
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(FarmSyncActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
                return;
            }
            FarmIdentity.setFolderUri(this, uri.toString());
            runSyncNow();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final FarmSyncManager.SyncResult result;
                    if (requestCode == REQ_JOIN_IMPORT) {
                        result = FarmSyncManager.adoptFromBackup(FarmSyncActivity.this, pendingJoinCode, uri);
                        pendingJoinCode = null;
                    } else {
                        result = FarmSyncManager.importBackup(FarmSyncActivity.this, uri);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FarmSyncActivity.this, result.message, Toast.LENGTH_LONG).show();
                            refresh();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FarmSyncActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
