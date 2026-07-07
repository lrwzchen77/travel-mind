<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';

const items = ref([]);
const total = ref(0);
const error = ref('');

async function load() {
  error.value = '';
  try {
    const data = await tripApi.history(20);
    items.value = data.items || [];
    total.value = data.total || items.value.length;
  } catch (err) {
    error.value = err?.message || 'Request failed';
  }
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <h1>Trip History</h1>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="table-wrap">
    <div class="table-meta">{{ total }} records</div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Title</th>
          <th>City</th>
          <th>Dates</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="items.length === 0">
          <td colspan="5">No records</td>
        </tr>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td><RouterLink :to="`/trip/${item.id}`">{{ item.title }}</RouterLink></td>
          <td>{{ item.destination_city }}</td>
          <td>{{ item.start_date }} - {{ item.end_date }}</td>
          <td>{{ item.status }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
