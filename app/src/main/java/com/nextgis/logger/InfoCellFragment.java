/*
 * *****************************************************************************
 * Project: NextGIS Logger
 * Purpose: Productive data logger for Android
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright © 2015 NextGIS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 */

package com.nextgis.logger;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoCellFragment extends Fragment implements CellEngine.CellInfoListener {
    private static final String CELL_ACTIVE     = "active";
    private static final String CELL_GEN        = "generation";
    private static final String CELL_TYPE       = "type";
    private static final String CELL_MCC        = "mcc";
    private static final String CELL_MNC        = "mnc";
    private static final String CELL_LAC        = "lac";
    private static final String CELL_CID        = "cid";
    private static final String CELL_PSC        = "psc";
    private static final String CELL_POWER      = "power";

    private CellEngine gsmEngine;

    private ListView lvNeighbours;

    @Override
    public void onPause() {
        super.onPause();

        gsmEngine.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        gsmEngine.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.info_cell_fragment, container, false);

        lvNeighbours = (ListView) rootView.findViewById(R.id.lv_neighbours);

        gsmEngine = new CellEngine(getActivity());
        gsmEngine.addCellListener(this);

        fillTextViews();

        return rootView;
    }

    private void fillTextViews() {
        ArrayList<CellEngine.GSMInfo> gsmInfoArray = gsmEngine.getGSMInfoArray();
        ArrayList<Map<String, Object>> neighboursData = new ArrayList<>();
        Map<String, Object> itemData;
        String na = getString(R.string.info_na);

        Collections.sort(gsmInfoArray, new Comparator<CellEngine.GSMInfo>() {
            @Override
            public int compare(CellEngine.GSMInfo lhs, CellEngine.GSMInfo rhs) {
                boolean b1 = lhs.isActive();
                boolean b2 = rhs.isActive();

                if( b1 && ! b2 ) {
                    return -1;
                }

                if( ! b1 && b2 ) {
                    return +1;
                }

                return 0;
            }
        });

        for (CellEngine.GSMInfo gsmItem : gsmInfoArray) {
            String gen, type, mnc, mcc, lac, cid, psc, power;

            gen = gsmItem.networkGen();
            gen = gen.equals("unknown") ? na : gen;
            type = gsmItem.networkType();
            type = type.equals("unknown") ? na : type;
            mcc = gsmItem.getMcc() == -1 ? na : gsmItem.getMcc() + "";
            mnc = gsmItem.getMnc() == -1 ? na : gsmItem.getMnc() + "";
            lac = gsmItem.getLac() == -1 ? na : gsmItem.getLac() + "";
            cid = gsmItem.getCid() == -1 ? na : gsmItem.getCid() + "";
            psc = gsmItem.getPsc() == -1 ? na : gsmItem.getPsc() + "";
            power = "" + gsmItem.getRssi() + " dBm";

            itemData = new HashMap<>();
            itemData.put(CELL_ACTIVE, gsmItem.isActive());
            itemData.put(CELL_GEN, gen);
            itemData.put(CELL_TYPE, type);
            itemData.put(CELL_MCC, mcc);
            itemData.put(CELL_MNC, mnc);
            itemData.put(CELL_LAC, lac);
            itemData.put(CELL_CID, cid);
            itemData.put(CELL_PSC, psc);
            itemData.put(CELL_POWER, power);
            neighboursData.add(itemData);
        }

        String[] from = { CELL_GEN, CELL_TYPE, CELL_MCC, CELL_MNC, CELL_LAC, CELL_CID, CELL_PSC, CELL_POWER };
        int[] to = {R.id.tv_network_gen, R.id.tv_network_type, R.id.tv_network_mcc, R.id.tv_network_mnc,
                R.id.tv_network_lac, R.id.tv_network_cid, R.id.tv_network_psc, R.id.tv_network_power};
        CellsAdapter saNeighbours = new CellsAdapter(getActivity(), neighboursData, R.layout.info_cell_neighbour_row, from, to);

        lvNeighbours.setAdapter(saNeighbours);
    }

    @Override
    public void onCellInfoChanged() {
        fillTextViews();
    }

    private class CellsAdapter extends SimpleAdapter {
        LayoutInflater mInflater;

        private TextView tvGen, tvType, tvOperator, tvMNC, tvMCC, tvLAC, tvCID, tvPSC, tvPower, tvNeighbours;

        /**
         * Constructor
         *
         * @param context  The context where the View associated with this SimpleAdapter is running
         * @param data     A List of Maps. Each entry in the List corresponds to one row in the list. The
         *                 Maps contain the data for each row, and should include all the entries specified in
         *                 "from"
         * @param resource Resource identifier of a view layout that defines the views for this list
         *                 item. The layout file should include at least those named views defined in "to"
         * @param from     A list of column names that will be added to the Map associated with each
         *                 item.
         * @param to       The views that should display column in the "from" parameter. These should all be
         *                 TextViews. The first N views in this list are given the values of the first N columns
         */
        public CellsAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                Map<String, ?> item = (Map<String, ?>) getItem(0);
                boolean isActive = (Boolean) item.get(CELL_ACTIVE);

                if (!isActive)
                    return super.getView(position, null, parent);

                View v = mInflater.inflate(R.layout.info_cell_active_row, parent, false);

                tvGen = (TextView) v.findViewById(R.id.tv_network_gen);
                tvType = (TextView) v.findViewById(R.id.tv_network_type);
                tvOperator = (TextView) v.findViewById(R.id.tv_network_operator);
                tvMCC = (TextView) v.findViewById(R.id.tv_network_mcc);
                tvMNC = (TextView) v.findViewById(R.id.tv_network_mnc);
                tvLAC = (TextView) v.findViewById(R.id.tv_network_lac);
                tvCID = (TextView) v.findViewById(R.id.tv_network_cid);
                tvPSC = (TextView) v.findViewById(R.id.tv_network_psc);
                tvPower = (TextView) v.findViewById(R.id.tv_network_power);
                tvNeighbours = (TextView) v.findViewById(R.id.tv_network_neighbours);

                tvOperator.setText(gsmEngine.getNetworkOperator());
                tvGen.setText((String) item.get(CELL_GEN));
                tvType.setText((String) item.get(CELL_TYPE));
                tvMCC.setText((String) item.get(CELL_MCC));
                tvMNC.setText((String) item.get(CELL_MNC));
                tvLAC.setText((String) item.get(CELL_LAC));
                tvCID.setText((String) item.get(CELL_CID));
                tvPSC.setText((String) item.get(CELL_PSC));
                tvPower.setText((String) item.get(CELL_POWER));

                int neighbours = getCount() - 1;
                tvNeighbours.setText("" + neighbours);

                return v;
            } else {
                if (convertView != null && convertView.findViewById(R.id.tv_network_neighbours) != null)
                    convertView = null;

                return super.getView(position, convertView, parent);
            }
        }
    }
}