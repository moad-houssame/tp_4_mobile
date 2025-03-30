package com.example.sqlite;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sqlite.classes.Etudiant;
import com.example.sqlite.service.EtudiantService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

public class StudentListActivity extends AppCompatActivity {
    private ListView studentList;
    private EtudiantService es;
    private List<Etudiant> students;
    private ArrayAdapter<String> adapter;
    private static final int PICK_IMAGE = 1;
    private String selectedImagePath;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Etudiant currentStudent; // To keep track of the student being edited


    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 (API 29-32)
            // No permission needed for scoped storage, but we need to handle URIs properly
            return true;
        } else {
            // Android 9 (API 28) and below
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    PERMISSION_REQUEST_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed for scoped storage
            loadStudents();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        if (!checkPermissions()) {
            requestPermissions();
        }

        es = new EtudiantService(this);
        studentList = findViewById(R.id.student_list);

        loadStudents();

        studentList.setOnItemClickListener((parent, view, position, id) -> showOptionsDialog(students.get(position)));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadStudents();
            } else {
                Toast.makeText(this, "Permission denied. Images won't be displayed.", Toast.LENGTH_SHORT).show();
                loadStudents(); // Still load students but without images
            }
        }
    }
    private void loadStudents() {
        students = es.findAll();
        if (students == null || students.isEmpty()) {
            Toast.makeText(this, "Aucun étudiant trouvé", Toast.LENGTH_SHORT).show();
            return;
        }
        CustomAdapter customAdapter = new CustomAdapter(this, students);
        studentList.setAdapter(customAdapter);
    }

    class CustomAdapter extends ArrayAdapter<Etudiant> {
        private Context context;
        private List<Etudiant> students;

        public CustomAdapter(Context context, List<Etudiant> students) {
            super(context, R.layout.list_item_student, students);
            this.context = context;
            this.students = students;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_item_student, parent, false);
            }

            // Get all views
            TextView studentName = view.findViewById(R.id.student_name);
            TextView studentPrenom = view.findViewById(R.id.student_prenom);
            TextView studentDate = view.findViewById(R.id.student_date);
            ImageView imageView = view.findViewById(R.id.student_image);

            Etudiant etudiant = students.get(position);

            // Set text values
            studentName.setText(etudiant.getNom());
            studentPrenom.setText(etudiant.getPrenom());
            studentDate.setText(etudiant.getDateNaissance());

            // Load image
            if (etudiant.getImagePath() != null && !etudiant.getImagePath().isEmpty()) {
                try {
                    Uri imageUri = Uri.parse(etudiant.getImagePath());
                    InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    imageView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    imageView.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_background);
            }

            return view;
        }
    }


    private void requestImagePermission(Uri imageUri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            // Reload the data after getting permission
            loadStudents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showOptionsDialog(Etudiant etudiant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choisissez une action")
                .setItems(new String[]{"Modifier", "Supprimer"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditPopup(etudiant);
                    } else {
                        confirmDelete(etudiant);
                    }
                })
                .show();
    }

    private void confirmDelete(Etudiant etudiant) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Voulez-vous vraiment supprimer cet étudiant?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    es.delete(etudiant);
                    Toast.makeText(this, "Étudiant supprimé", Toast.LENGTH_SHORT).show();
                    loadStudents();
                })
                .setNegativeButton("Non", null)
                .show();
    }
    private String currentImagePath; // Add this at the class level

    private Uri selectedImageUri;    // Store the selected image URI
    private ImageView selectedImageView; // Reference to the current ImageView

    // In StudentListActivity.java

    private void showEditPopup(Etudiant etudiant) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View popupView = LayoutInflater.from(this).inflate(R.layout.edit_student_popup, null);
        dialog.setContentView(popupView);

        // Initialize views
        EditText editNom = popupView.findViewById(R.id.edit_nom);
        EditText editPrenom = popupView.findViewById(R.id.edit_prenom);
        EditText editDateNaissance = popupView.findViewById(R.id.edit_date_naissance);
        ImageView imageView = popupView.findViewById(R.id.selected_image);
        Button selectImageButton = popupView.findViewById(R.id.btn_select_image);
        Button deleteImageButton = popupView.findViewById(R.id.btn_delete_image);
        Button saveButton = popupView.findViewById(R.id.btn_save);

        // Set current values
        editNom.setText(etudiant.getNom());
        editPrenom.setText(etudiant.getPrenom());
        editDateNaissance.setText(etudiant.getDateNaissance());

        // Load current image
        if (etudiant.getImagePath() != null && !etudiant.getImagePath().isEmpty()) {
            try {
                Uri imageUri = Uri.parse(etudiant.getImagePath());
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
                deleteImageButton.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_launcher_background);
                deleteImageButton.setVisibility(View.GONE);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background);
            deleteImageButton.setVisibility(View.GONE);
        }

        // Date picker
        editDateNaissance.setFocusable(false);
        editDateNaissance.setOnClickListener(v -> showDatePicker(editDateNaissance));

        // Image selection - Using gallery picker
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
            selectedImageView = imageView;  // Store reference to update later
        });

        // Image deletion
        deleteImageButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Voulez-vous vraiment supprimer cette image ?")
                    .setPositiveButton("Oui", (dialog1, which) -> {
                        selectedImagePath = null;
                        imageView.setImageResource(R.drawable.ic_launcher_background);
                        deleteImageButton.setVisibility(View.GONE);
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });

        // Save changes
        saveButton.setOnClickListener(v -> {
            if (editNom.getText().toString().isEmpty() ||
                    editPrenom.getText().toString().isEmpty() ||
                    editDateNaissance.getText().toString().isEmpty()) {
                Toast.makeText(this, "Veuillez compléter tous les champs", Toast.LENGTH_SHORT).show();
            } else {
                etudiant.setNom(editNom.getText().toString());
                etudiant.setPrenom(editPrenom.getText().toString());
                etudiant.setDateNaissance(editDateNaissance.getText().toString());

                // Update image path if changed
                if (selectedImagePath != null) {
                    etudiant.setImagePath(selectedImagePath);
                }

                es.update(etudiant);
                Toast.makeText(this, "Étudiant modifié avec succès", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadStudents();
            }
        });

        // Set dialog dimensions and show
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }



    // Helper method to load image
    private void loadImageIntoView(ImageView imageView, String imagePath, Button deleteButton) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imagePath);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
                if (deleteButton != null) {
                    deleteButton.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.ic_launcher_background);
                if (deleteButton != null) {
                    deleteButton.setVisibility(View.GONE);
                }
            }
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background);
            if (deleteButton != null) {
                deleteButton.setVisibility(View.GONE);
            }
        }
    }
    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> editText.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            selectedImagePath = selectedImageUri.toString();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                if (selectedImageView != null) {
                    selectedImageView.setImageBitmap(bitmap);

                    // Show delete button
                    View deleteButton = ((View)selectedImageView.getParent())
                            .findViewById(R.id.btn_delete_image);
                    if (deleteButton != null) {
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur de chargement de l'image", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
