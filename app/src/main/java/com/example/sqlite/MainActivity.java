package com.example.sqlite;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.sqlite.classes.Etudiant;
import com.example.sqlite.service.EtudiantService;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;  // Single constant for image picking
    private EtudiantService es;
    private String imagePath;
    private ImageView selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        es = new EtudiantService(this);
        setupViews();
    }

    private void setupViews() {
        // Initialize views
        EditText nom = findViewById(R.id.nom);
        EditText prenom = findViewById(R.id.prenom);
        EditText id = findViewById(R.id.id);
        TextView selectedDate = findViewById(R.id.selected_date);
        selectedImage = findViewById(R.id.selected_image);  // Initialize the ImageView

        // Date picker
        findViewById(R.id.pick_date).setOnClickListener(v -> showDatePicker(selectedDate));

        // Image selection - now using gallery picker
        findViewById(R.id.pick_image).setOnClickListener(v -> pickImageFromGallery());

        // Add student
        findViewById(R.id.bn).setOnClickListener(v -> addStudent(nom, prenom, selectedDate));

        // Search student
        findViewById(R.id.load).setOnClickListener(v -> searchStudent(id));

        // View all students
        findViewById(R.id.view_students).setOnClickListener(v ->
                startActivity(new Intent(this, StudentListActivity.class)));
    }

    private void showDatePicker(TextView dateView) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                dateView.setText(d + "/" + (m+1) + "/" + y),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void addStudent(EditText nom, EditText prenom, TextView date) {
        if (validateInput(nom, prenom, date)) {
            es.create(new Etudiant(
                    nom.getText().toString(),
                    prenom.getText().toString(),
                    date.getText().toString(),
                    imagePath
            ));
            clearForm(nom, prenom, date);
            Toast.makeText(this, "Étudiant ajouté", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Remplissez tous les champs", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(EditText nom, EditText prenom, TextView date) {
        return !nom.getText().toString().isEmpty() &&
                !prenom.getText().toString().isEmpty() &&
                !date.getText().toString().equals("Sélectionner une date") &&
                imagePath != null;
    }

    private void clearForm(EditText nom, EditText prenom, TextView date) {
        nom.setText("");
        prenom.setText("");
        date.setText("Sélectionner une date");
        selectedImage.setImageBitmap(null);
        imagePath = null;
    }

    private void searchStudent(EditText idInput) {
        String id = idInput.getText().toString();
        if (id.isEmpty()) {
            Toast.makeText(this, "Entrez un ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Etudiant e = es.findById(Integer.parseInt(id));
        TextView result = findViewById(R.id.res);
        result.setText(e != null ?
                e.getNom() + " " + e.getPrenom() + "\n" + e.getDateNaissance() :
                "Étudiant introuvable");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                imagePath = selectedImageUri.toString();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    selectedImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Échec du chargement de l'image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}