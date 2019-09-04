
package com.etoitau.giftessera.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.etoitau.giftessera.FilesActivity;
import com.etoitau.giftessera.R;
import com.etoitau.giftessera.domain.DatabaseFile;

import java.util.List;

/**
 * Adapter for recyclerview to show Others
 * Has three modes for showing: collected files, burned files, or files not categorized (browse)
 */
public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
    private List<DatabaseFile> files; // the data model
    FilesActivity filesActivity; // the calling activity

    // mode set on creation to one of three types of adapter
    private final int MODE;
    public static final int SAVING = 0, LOADING = 1;

    // takes calling activity, data model, and desired display mode
    public FilesAdapter(FilesActivity activity, List<DatabaseFile> files, int mode) {
        this.files = files;
        this.filesActivity = activity;
        this.MODE = mode;
    }

    // Inflating a layout from XML and returning the holder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View otherView = inflater.inflate(R.layout.file_entry, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(otherView);
        return viewHolder;
    }

    // Populating data into the item through holder
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        // Get the data model based on position
        final DatabaseFile databaseFile = files.get(position);

        // Set up delete button,
        final ImageButton del = holder.delButton;

        del.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               filesActivity.deleteFileAlert(databaseFile);
           }
        });

        // set up filename display
        TextView fileNameView = holder.fileName;
        fileNameView.setText(databaseFile.getName());

        // add listener to filename to either save to or load this file
        if (MODE == SAVING) {
            fileNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    filesActivity.overwriteFileAlert(databaseFile);
                }
            });
        } else {
            fileNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    filesActivity.loadFileAlert(databaseFile);
                }
            });
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return files.size();
    }

    // ViewHolder Object - get parts of xml item layout
    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageButton delButton;
        public TextView fileName;

        public ViewHolder(View itemView) {
            super(itemView);
            this.delButton = itemView.findViewById(R.id.fileDelButton);
            this.fileName = itemView.findViewById(R.id.entryFileName);
        }
    }
}
